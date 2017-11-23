package capsule.structure;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;

public class CapsulePlacementSettings extends PlacementSettings
{
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private boolean ignoreEntities;
    /** the type of block in the world that will get replaced by the structure */
    @Nullable
    private Block replacedBlock;
    /** the chunk the structure is within */
    @Nullable
    private ChunkPos chunk;
    /** the bounds the structure is contained within */
    @Nullable
    private StructureBoundingBox boundingBox;
    private boolean ignoreStructureBlock = true;
    private float integrity = 1.0F;
    @Nullable
    private Random random;
    @Nullable
    private Long setSeed;

    public CapsulePlacementSettings copy()
    {
        CapsulePlacementSettings CapsulePlacementSettings = new CapsulePlacementSettings();
        CapsulePlacementSettings.mirror = this.mirror;
        CapsulePlacementSettings.rotation = this.rotation;
        CapsulePlacementSettings.ignoreEntities = this.ignoreEntities;
        CapsulePlacementSettings.replacedBlock = this.replacedBlock;
        CapsulePlacementSettings.chunk = this.chunk;
        CapsulePlacementSettings.boundingBox = this.boundingBox;
        CapsulePlacementSettings.ignoreStructureBlock = this.ignoreStructureBlock;
        CapsulePlacementSettings.integrity = this.integrity;
        CapsulePlacementSettings.random = this.random;
        CapsulePlacementSettings.setSeed = this.setSeed;
        return CapsulePlacementSettings;
    }

    public CapsulePlacementSettings setMirror(Mirror mirrorIn)
    {
        this.mirror = mirrorIn;
        return this;
    }

    public CapsulePlacementSettings setRotation(Rotation rotationIn)
    {
        this.rotation = rotationIn;
        return this;
    }

    public CapsulePlacementSettings setIgnoreEntities(boolean ignoreEntitiesIn)
    {
        this.ignoreEntities = ignoreEntitiesIn;
        return this;
    }

    public CapsulePlacementSettings setReplacedBlock(Block replacedBlockIn)
    {
        this.replacedBlock = replacedBlockIn;
        return this;
    }

    public CapsulePlacementSettings setChunk(ChunkPos chunkPosIn)
    {
        this.chunk = chunkPosIn;
        return this;
    }

    public CapsulePlacementSettings setBoundingBox(StructureBoundingBox boundingBoxIn)
    {
        this.boundingBox = boundingBoxIn;
        return this;
    }

    public CapsulePlacementSettings setSeed(@Nullable Long seedIn)
    {
        this.setSeed = seedIn;
        return this;
    }

    public CapsulePlacementSettings setRandom(@Nullable Random randomIn)
    {
        this.random = randomIn;
        return this;
    }

    public CapsulePlacementSettings setIntegrity(float integrityIn)
    {
        this.integrity = integrityIn;
        return this;
    }

    public Mirror getMirror()
    {
        return this.mirror;
    }

    public CapsulePlacementSettings setIgnoreStructureBlock(boolean ignoreStructureBlockIn)
    {
        this.ignoreStructureBlock = ignoreStructureBlockIn;
        return this;
    }

    public Rotation getRotation()
    {
        return this.rotation;
    }

    public Random getRandom(@Nullable BlockPos seed)
    {
        if (this.random != null)
        {
            return this.random;
        }
        else if (this.setSeed != null)
        {
            return this.setSeed.longValue() == 0L ? new Random(System.currentTimeMillis()) : new Random(this.setSeed.longValue());
        }
        else if (seed == null)
        {
            return new Random(System.currentTimeMillis());
        }
        else
        {
            int i = seed.getX();
            int j = seed.getZ();
            return new Random((long)(i * i * 4987142 + i * 5947611) + (long)(j * j) * 4392871L + (long)(j * 389711) ^ 987234911L);
        }
    }

    public float getIntegrity()
    {
        return this.integrity;
    }

    public boolean getIgnoreEntities()
    {
        return this.ignoreEntities;
    }

    @Nullable
    public Block getReplacedBlock()
    {
        return this.replacedBlock;
    }

    @Nullable
    public StructureBoundingBox getBoundingBox()
    {
        if (this.boundingBox == null && this.chunk != null)
        {
            this.setBoundingBoxFromChunk();
        }

        return this.boundingBox;
    }

    public boolean getIgnoreStructureBlock()
    {
        return this.ignoreStructureBlock;
    }

    void setBoundingBoxFromChunk()
    {
        this.boundingBox = this.getBoundingBoxFromChunk(this.chunk);
    }

    @Nullable
    private StructureBoundingBox getBoundingBoxFromChunk(@Nullable ChunkPos pos)
    {
        if (pos == null)
        {
            return null;
        }
        else
        {
            int i = pos.x * 16;
            int j = pos.z * 16;
            return new StructureBoundingBox(i, 0, j, i + 16 - 1, 255, j + 16 - 1);
        }
    }
}