## Plan

1. Define the converter inputs and outputs
   - Use the 26.3 client JAR as the source for vanilla block states, models, and textures.
   - Keep downloaded and converted assets in a versioned cache outside the repository.
   - Emit an atlas image plus metadata that maps each block state and face to an atlas region.
2. Build the asset conversion step
   - Read block state definitions and resolve their model variants.
   - Follow model parents and texture aliases to find each face’s texture.
   - Pack the referenced textures into an atlas and write the state-to-face lookup.
   - Report unresolved states or textures so missing mappings are visible.
3. Connect it to the renderer
   - Load the generated atlas and lookup when entering a Minecraft world.
   - Extend voxel mesh generation to carry per-face UVs and group faces by texture.
   - Keep existing flat-color materials for legacy models and use a fallback for states without textures.
4. Validate the result
   - Check representative blocks with different face textures and state variants, such as grass, logs, stairs, and water.
   - Confirm chunk updates and edits still work with textured meshes.
   - Measure atlas size and mesh cost on the currently loaded server view.
5. Add resource-pack support
   - Once vanilla rendering works, apply a server-provided pack as an override layer and rebuild the cache for that connection.