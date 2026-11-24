const CAPABILITIES = Object.freeze([
  Object.freeze({ name: "bluecarbon.knowledge.search", visibility: "PUBLIC", readOnly: true }),
  Object.freeze({ name: "bluecarbon.region.metrics.read", visibility: "PUBLIC", readOnly: true }),
  Object.freeze({ name: "bluecarbon.methodology.lookup", visibility: "PUBLIC", readOnly: true }),
  Object.freeze({ name: "bluecarbon.source.verify", visibility: "PUBLIC", readOnly: true }),
]);

export function capabilitySnapshot() {
  return {
    service: "HNBLUE",
    datasetIdentity: "HNBLUE_CAPABILITY_METADATA_V1",
    synthetic: false,
    capabilities: CAPABILITIES.map((item) => ({ ...item })),
    deniedCapabilities: ["database.write", "workorder.approve", "account.manage", "shell.execute", "arbitrary.fetch"],
  };
}
