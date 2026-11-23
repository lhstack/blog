export const rememberedCredentialsKey = 'blog-remembered-login-v1'

export function clearRememberedCredentials() {
  try {
    window.localStorage.removeItem(rememberedCredentialsKey)
    return true
  } catch {
    return false
  }
}
