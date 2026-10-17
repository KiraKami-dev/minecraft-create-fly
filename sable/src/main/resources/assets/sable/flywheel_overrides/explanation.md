Sable's sub-levels each contain their own lighting sections, and lighting data.

We need Flywheel shaders to be aware of this, so we change and override the lighting storage, LUT, and shaders to respect an additional "scene ID".

I'm not happy with the large amounts of duplicated shader code in these overrides, but it's the route we are going with for now.

Reference https://github.com/Engine-Room/Flywheel/tree/1.21.1/dev for the original shaders and lighting code that these overrides are based on.

Fabric port: the Flywheel used here is the one bundled in Create Fly (`com.zurrtum.create.client.flywheel`). These files are
Create Fly's copies of `assets/flywheel/flywheel/<path>` with Sable's changes applied (Create Fly's fog distance outputs
are kept). They live under `assets/sable/flywheel_overrides/<path>` and `ShaderSourcesMixin` makes Flywheel's shader
loader read them in place of its own, since Fabric doesn't guarantee which mod's copy of a resource wins.
