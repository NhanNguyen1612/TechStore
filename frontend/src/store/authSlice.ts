import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { api, apiData } from "../lib/api";
import { authStorage } from "../lib/authStorage";
import { getErrorMessage } from "../lib/format";
import type { AuthPayload, UserProfile } from "../types/api";

interface AuthState {
  user: UserProfile | null;
  initialized: boolean;
  loading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: null,
  initialized: false,
  loading: false,
  error: null,
};

const loadCurrentUser = () =>
  apiData<UserProfile>(api.get("/api/users/me"));

export const bootstrapAuth = createAsyncThunk(
  "auth/bootstrap",
  async (_, { rejectWithValue }) => {
    if (!authStorage.getAccessToken() && !authStorage.getRefreshToken()) {
      return null;
    }
    try {
      return await loadCurrentUser();
    } catch (error) {
      authStorage.clear();
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const login = createAsyncThunk(
  "auth/login",
  async (
    credentials: { email: string; password: string },
    { rejectWithValue },
  ) => {
    try {
      const payload = await apiData<AuthPayload>(
        api.post("/api/auth/login", credentials),
      );
      authStorage.setTokens(payload.accessToken, payload.refreshToken);
      return await loadCurrentUser();
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const register = createAsyncThunk(
  "auth/register",
  async (
    input: {
      email: string;
      password: string;
      fullName: string;
      phone: string;
    },
    { rejectWithValue },
  ) => {
    try {
      const payload = await apiData<AuthPayload>(
        api.post("/api/auth/register", input),
      );
      authStorage.setTokens(payload.accessToken, payload.refreshToken);
      return await loadCurrentUser();
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const updateProfile = createAsyncThunk(
  "auth/updateProfile",
  async (
    input: { fullName: string; phone: string },
    { rejectWithValue },
  ) => {
    try {
      await apiData<UserProfile>(api.put("/api/auth/profile", input));
      return await loadCurrentUser();
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const uploadAvatar = createAsyncThunk(
  "auth/uploadAvatar",
  async (file: File, { rejectWithValue }) => {
    try {
      const form = new FormData();
      form.append("avatar", file);
      await api.put("/api/users/avatar", form);
      return await loadCurrentUser();
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const changePassword = createAsyncThunk(
  "auth/changePassword",
  async (
    input: {
      currentPassword: string;
      newPassword: string;
      confirmPassword: string;
    },
    { rejectWithValue },
  ) => {
    try {
      await api.put("/api/auth/change-password", input);
      authStorage.clear();
      return true;
    } catch (error) {
      return rejectWithValue(getErrorMessage(error));
    }
  },
);

export const logout = createAsyncThunk("auth/logout", async () => {
  try {
    await api.post("/api/auth/logout");
  } finally {
    authStorage.clear();
  }
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    clearAuthError: (state) => {
      state.error = null;
    },
    sessionExpired: (state) => {
      state.user = null;
      state.initialized = true;
      state.loading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(bootstrapAuth.pending, (state) => {
        state.loading = true;
      })
      .addCase(bootstrapAuth.fulfilled, (state, action) => {
        state.user = action.payload;
        state.initialized = true;
        state.loading = false;
      })
      .addCase(bootstrapAuth.rejected, (state) => {
        state.user = null;
        state.initialized = true;
        state.loading = false;
      })
      .addCase(login.pending, pending)
      .addCase(login.fulfilled, authenticated)
      .addCase(login.rejected, rejected)
      .addCase(register.pending, pending)
      .addCase(register.fulfilled, authenticated)
      .addCase(register.rejected, rejected)
      .addCase(updateProfile.pending, pending)
      .addCase(updateProfile.fulfilled, authenticated)
      .addCase(updateProfile.rejected, rejected)
      .addCase(uploadAvatar.pending, pending)
      .addCase(uploadAvatar.fulfilled, authenticated)
      .addCase(uploadAvatar.rejected, rejected)
      .addCase(changePassword.pending, pending)
      .addCase(changePassword.fulfilled, (state) => {
        state.user = null;
        state.loading = false;
      })
      .addCase(changePassword.rejected, rejected)
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.loading = false;
      });
  },
});

function pending(state: AuthState) {
  state.loading = true;
  state.error = null;
}

function authenticated(
  state: AuthState,
  action: { payload: UserProfile },
) {
  state.user = action.payload;
  state.loading = false;
  state.initialized = true;
  state.error = null;
}

function rejected(
  state: AuthState,
  action: { payload?: unknown; error: { message?: string } },
) {
  state.loading = false;
  state.error =
    typeof action.payload === "string"
      ? action.payload
      : action.error.message || "Yêu cầu thất bại";
}

export const { clearAuthError, sessionExpired } = authSlice.actions;
export default authSlice.reducer;
