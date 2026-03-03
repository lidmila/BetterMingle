const fs = require("fs");
const path = require("path");
const c = JSON.parse(fs.readFileSync(path.join(process.env.USERPROFILE, ".config/configstore/firebase-tools.json"), "utf8"));
const t = c.tokens;
console.log("keys:", Object.keys(t));
console.log("refresh_token exists:", typeof t.refresh_token === "string");
