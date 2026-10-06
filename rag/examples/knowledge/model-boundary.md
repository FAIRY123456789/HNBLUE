# Model service boundary

## 中文摘要

模型预测只用于演示推理、解释和服务集成，不等于现场测量、正式核算或已核证碳信用。使用结果前仍需确认单位换算、适用区域、密度与碳系数、本地校准、不确定性和独立验证；缺少这些条件时应展示限制，不得把预测写成确定事实。

## Demonstration inputs

The CatBoost service accepts structural, environmental, location, and categorical features such as tree height, diameter, canopy area, age, latitude, longitude, climate proxies, vegetation class, growing condition, and plant functional type.

## Interpretation

The serialized pipeline demonstrates inference and explanation integration. A prediction is a model output, not a field measurement, causal ecological finding, per-hectare carbon stock, verified credit, or legal accounting result. Unit conversion, stand density, carbon fraction, local calibration, uncertainty, and independent validation remain separate responsibilities.

## Explainability

SHAP and sensitivity views describe how this trained model responds to inputs. They do not establish ecological causality and should not be presented as policy evidence without domain review.
