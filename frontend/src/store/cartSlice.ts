import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { api, apiData } from "../lib/api";
import { getErrorMessage } from "../lib/format";
import type { Cart } from "../types/api";

interface CartState {
  data: Cart;
  loading: boolean;
  error: string | null;
}

const emptyCart: Cart = { items: [], totalQuantity: 0, totalAmount: 0 };

const initialState: CartState = {
  data: emptyCart,
  loading: false,
  error: null,
};

export const fetchCart = createAsyncThunk(
  "cart/fetch",
  async (_, { rejectWithValue }) => {
    try {
      return await apiData<Cart>(api.get("/api/cart"));
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const addToCart = createAsyncThunk(
  "cart/add",
  async (
    input: { productId: number; quantity: number },
    { rejectWithValue },
  ) => {
    try {
      return await apiData<Cart>(api.post("/api/cart/add", input));
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const updateCartItem = createAsyncThunk(
  "cart/update",
  async (
    input: { productId: number; quantity: number },
    { rejectWithValue },
  ) => {
    try {
      return await apiData<Cart>(api.put("/api/cart/update", input));
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const removeCartItem = createAsyncThunk(
  "cart/remove",
  async (productId: number, { rejectWithValue }) => {
    try {
      return await apiData<Cart>(
        api.delete("/api/cart/remove", { data: { productId } }),
      );
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const clearCart = createAsyncThunk(
  "cart/clear",
  async (_, { rejectWithValue }) => {
    try {
      return await apiData<Cart>(api.delete("/api/cart/clear"));
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

const cartSlice = createSlice({
  name: "cart",
  initialState,
  reducers: {
    resetCart: (state) => {
      state.data = emptyCart;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    for (const thunk of [
      fetchCart,
      addToCart,
      updateCartItem,
      removeCartItem,
      clearCart,
    ]) {
      builder
        .addCase(thunk.pending, (state) => {
          state.loading = true;
          state.error = null;
        })
        .addCase(thunk.fulfilled, (state, action) => {
          state.data = action.payload;
          state.loading = false;
        })
        .addCase(thunk.rejected, (state, action) => {
          state.loading = false;
          state.error =
            typeof action.payload === "string"
              ? action.payload
              : "Không thể xử lý giỏ hàng";
        });
    }
  },
});

export const { resetCart } = cartSlice.actions;
export default cartSlice.reducer;
