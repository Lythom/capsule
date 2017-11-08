package capsule.structure;

import net.minecraft.block.Block;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;

import javax.annotation.Nullable;
import java.util.Random;

public class CapsulePlacementSettings extends PlacementSettings {
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private boolean ignoreEntities = false;
    /**
     * the type of block in the world that will get replaced by the structure
     */
    @Nullable
    private Block replacedBlock;
    /**
     * the chunk the structure is within
     */
    @Nullable
    private ChunkPos chunk;
    /**
     * the bounds the structure is contained within
     */
    @Nullable
    private StructureBoundingBox boundingBox;
    private boolean ignoreStructureBlock = true;
    private float field_189951_h = 1.0F;
    @Nullable
    private Random field_189952_i;
    @Nullable
    private Long field_189953_j;

    public CapsulePlacementSettings copy() {
        CapsulePlacementSettings placementsettings = new CapsulePlacementSettings();
        placementsettings.mirror = this.mirror;
        placementsettings.rotation = this.rotation;
        placementsettings.ignoreEntities = this.ignoreEntities;
        placementsettings.replacedBlock = this.replacedBlock;
        placementsettings.chunk = this.chunk;
        placementsettings.boundingBox = this.boundingBox;
        placementsettings.ignoreStructureBlock = this.ignoreStructureBlock;
        placementsettings.field_189951_h = this.field_189951_h;
        placementsettings.field_189952_i = this.field_189952_i;
        placementsettings.field_189953_j = this.field_189953_j;
        return placementsettings;
    }

    public CapsulePlacementSettings setChunk(ChunkPos chunkPosIn) {
        this.chunk = chunkPosIn;
        return this;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public CapsulePlacementSettings setMirror(Mirror mirrorIn) {
        this.mirror = mirrorIn;
        return this;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public CapsulePlacementSettings setRotation(Rotation rotationIn) {
        this.rotation = rotationIn;
        return this;
    }

    public boolean getIgnoreEntities() {
        return this.ignoreEntities;
    }

    public CapsulePlacementSettings setIgnoreEntities(boolean ignoreEntitiesIn) {
        this.ignoreEntities = ignoreEntitiesIn;
        return this;
    }

    @Nullable
    public Block getReplacedBlock() {
        return this.replacedBlock;
    }

    public CapsulePlacementSettings setReplacedBlock(Block replacedBlockIn) {
        this.replacedBlock = replacedBlockIn;
        return this;
    }

    @Nullable
    public StructureBoundingBox getBoundingBox() {
        if (this.boundingBox == null && this.chunk != null) {
            this.setBoundingBoxFromChunk();
        }

        return this.boundingBox;
    }

    public CapsulePlacementSettings setBoundingBox(StructureBoundingBox boundingBoxIn) {
        this.boundingBox = boundingBoxIn;
        return this;
    }

    public boolean getIgnoreStructureBlock() {
        return this.ignoreStructureBlock;
    }

    public CapsulePlacementSettings setIgnoreStructureBlock(boolean ignoreStructureBlockIn) {
        this.ignoreStructureBlock = ignoreStructureBlockIn;
        return this;
    }

    public void setBoundingBoxFromChunk() {
        this.boundingBox = this.getBoundingBoxFromChunk(this.chunk);
    }

    @Nullable
    private StructureBoundingBox getBoundingBoxFromChunk(@Nullable ChunkPos pos) {
        if (pos == null) {
            return null;
        } else {
            int i = pos.chunkXPos * 16;
            int j = pos.chunkZPos * 16;
            return new StructureBoundingBox(i, 0, j, i + 16 - 1, 255, j + 16 - 1);
        }
    }
}