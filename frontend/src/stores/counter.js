import { createStore } from "vuex";

export default createStore({
  state() {
    return {
      userType: localStorage.getItem("userType") || "User",
    };
  },
  mutations: {
    setUserType(state, userType) {
      state.userType = userType || "User";
      localStorage.setItem("userType", state.userType);
    },
  },
});
