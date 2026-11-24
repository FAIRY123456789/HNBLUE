import test from "node:test";
import assert from "node:assert/strict";
import { capabilitySnapshot } from "../src/core.js";

test("lab exposes only read-only public capability metadata", () => {
  const result = capabilitySnapshot();
  assert.equal(result.datasetIdentity, "HNBLUE_CAPABILITY_METADATA_V1");
  assert.equal(result.capabilities.length, 4);
  assert.ok(result.capabilities.every((item) => item.readOnly && item.visibility === "PUBLIC"));
  assert.ok(result.deniedCapabilities.includes("database.write"));
  assert.ok(result.deniedCapabilities.includes("shell.execute"));
  assert.equal(JSON.stringify(result).includes("password"), false);
});
