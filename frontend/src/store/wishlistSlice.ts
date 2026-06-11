import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { api, apiData } from "../lib/api";
import { getErrorMessage } from "../lib/format";
import type { Wishlist, WishlistItem } from "../types/api";

interface WishlistState {
  items: WishlistItem[];
  loading: boolean;
  error: string | null;
}

const initialState: WishlistState = {
  items: [],
  loading: false,
  error: null,
};

export const fetchWishlist = createAsyncThunk(
  "wishlist/fetch",
  async (_, { rejectWithValue }) => {
    try {
      return await apiData<Wishlist>(api.get("/api/wishlist"));
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const addWishlist = createAsyncThunk(
  "wishlist/add",
  async (productId: number, { rejectWithValue }) => {
    try {
      return await apiData<WishlistItem>(
        api.post(`/api/wishlist/${productId}`),
      );
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const removeWishlist = createAsyncThunk(
  "wishlist/remove",
  async (productId: number, { rejectWithValue }) => {
    try {
      await api.delete(`/api/wishlist/${productId}`);
      return productId;
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

const wishlistSlice = createSlice({
  name: "wishlist",
  initialState,
  reducers: {
    resetWishlist: (state) => {
      state.items = [];
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWishlist.pending, pending)
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.items = action.payload.items;
        state.loading = false;
      })
      .addCase(fetchWishlist.rejected, rejected)
      .addCase(addWishlist.pending, pending)
      .addCase(addWishlist.fulfilled, (state, action) => {
        if (!state.items.some((item) => item.productId === action.payload.productId)) {
          state.items.unshift(action.payload);
        }
        state.loading = false;
      })
      .addCase(addWishlist.rejected, rejected)
      .addCase(removeWishlist.pending, pending)
      .addCase(removeWishlist.fulfilled, (state, action) => {
        state.items = state.items.filter(
          (item) => item.productId !== action.payload,
        );
        state.loading = false;
      })
      .addCase(removeWishlist.rejected, rejected);
  },
});

function pending(state: WishlistState) {
  state.loading = true;
  state.error = null;
}

function rejected(
  state: WishlistState,
  action: { payload?: unknown },
) {
  state.loading = false;
  state.error =
    typeof action.payload === "string"
      ? action.payload
      : "Không thể xử lý danh sách yêu thích";
}

export const { resetWishlist } = wishlistSlice.actions;
export default wishlistSlice.reducer;
