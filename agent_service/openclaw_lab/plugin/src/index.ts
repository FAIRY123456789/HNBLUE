import { defineToolPlugin } from "openclaw/plugin-sdk/tool-plugin";
import { Type } from "typebox";
import { capabilitySnapshot } from "./core.js";

export default defineToolPlugin({
  id: "hnblue-bluecarbon-readonly-lab",
  name: "HNBLUE Blue Carbon Read-only Lab",
  description: "Returns static public capability metadata; it cannot read private data or perform writes.",
  tools: (tool) => [
    tool({
      name: "hnblue_bluecarbon_capabilities",
      label: "HNBLUE Blue Carbon Capabilities",
      description: "List the isolated HNBLUE read-only capability contract and explicit denials.",
      parameters: Type.Object({}, { additionalProperties: false }),
      outputSchema: Type.Object({
        service: Type.String(),
        datasetIdentity: Type.String(),
        synthetic: Type.Boolean(),
        capabilities: Type.Array(Type.Object({
          name: Type.String(), visibility: Type.String(), readOnly: Type.Boolean(),
        }, { additionalProperties: false })),
        deniedCapabilities: Type.Array(Type.String()),
      }, { additionalProperties: false }),
      optional: true,
      execute() {
        return capabilitySnapshot();
      },
    }),
  ],
});
