# CartCompare Personal

Private Android grocery comparison app.

- Grocery list and saved prices stay on the phone.
- Opens Kroger, Meijer, Walmart, Target, Aldi, Giant Eagle, BJ's, Costco and Sam's Club in an in-app WebView.
- You sign in yourself; CartCompare does not ask for or store retailer passwords.
- Device autofill/password manager may offer saved credentials where supported.
- Tap **Save price** while viewing a retailer page to record the brand, exact product, package size and price.
- Best Trip groups each item by its lowest saved price.
- Print/PDF shopping list includes store, brand, exact product, package size, requested quantity and price.
- No ads, analytics, API keys or CartCompare backend.

## Current limitation
The first build does not automatically scrape logged-in retailer pages. Price capture is manual/semi-automated.

## Build with GitHub Actions
Push this project to a GitHub repo. The included workflow builds a debug APK and uploads it as an Actions artifact named `CartCompare-Personal-APK`.
