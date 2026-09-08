# HNBLUE example data

This directory contains a tiny **synthetic** package that mirrors the file
layout expected by `ExternalDatasetService`. It is safe to keep in Git and is
only intended for interface demonstrations and parser tests.

The rows are not copied from BAAD, Tallo, ChinAllomeTree, or GWM and must not
be used for scientific analysis, model evaluation, or carbon accounting.

For a local demo, set:

```text
HNBLUE_EXTERNAL_DATA_ROOT=data/examples
```

When the backend is started from `backend/`, the service also discovers the
repository-level `data/examples` directory automatically when `data/raw` is
absent. Licensed or full datasets belong under the ignored `data/raw` tree.
