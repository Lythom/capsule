package capsule.structure;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityStructure;
import net.minecraft.util.Mirror;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.Rotation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraft.util.datafix.IDataWalker;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.BlockRotationProcessor;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.Template;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Exact copy of mc original net.minecraft.world.gen.structure.template.Template class, but having fields public to allow external manipulation
 * @author Lythom
 */
public class CapsuleTemplate
{
	public final List<Template.BlockInfo> blocks = Lists.<Template.BlockInfo>newArrayList();
	public final List<Template.EntityInfo> entities = Lists.<Template.EntityInfo>newArrayList();
    /** size of the structure */
    public BlockPos size = BlockPos.ORIGIN;
    /** The author of this template. */
    public String author = "?";

    public BlockPos getSize()
    {
        return this.size;
    }

    public void setAuthor(String authorIn)
    {
        this.author = authorIn;
    }

    public String getAuthor()
    {
        return this.author;
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    public void takeBlocksFromWorld(World worldIn, BlockPos startPos, BlockPos endPos, boolean takeEntities, @Nullable Block toIgnore)
    {
        if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1)
        {
            BlockPos blockpos = startPos.add(endPos).add(-1, -1, -1);
            List<Template.BlockInfo> list = Lists.<Template.BlockInfo>newArrayList();
            List<Template.BlockInfo> list1 = Lists.<Template.BlockInfo>newArrayList();
            List<Template.BlockInfo> list2 = Lists.<Template.BlockInfo>newArrayList();
            BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()), Math.min(startPos.getZ(), blockpos.getZ()));
            BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()), Math.max(startPos.getZ(), blockpos.getZ()));
            this.size = endPos;

            for (BlockPos.MutableBlockPos blockpos$mutableblockpos : BlockPos.getAllInBoxMutable(blockpos1, blockpos2))
            {
                BlockPos blockpos3 = blockpos$mutableblockpos.subtract(blockpos1);
                IBlockState iblockstate = worldIn.getBlockState(blockpos$mutableblockpos);

                if (toIgnore == null || toIgnore != iblockstate.getBlock())
                {
                    TileEntity tileentity = worldIn.getTileEntity(blockpos$mutableblockpos);

                    if (tileentity != null)
                    {
                        NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());
                        nbttagcompound.removeTag("x");
                        nbttagcompound.removeTag("y");
                        nbttagcompound.removeTag("z");
                        list1.add(new Template.BlockInfo(blockpos3, iblockstate, nbttagcompound));
                    }
                    else if (!iblockstate.isFullBlock() && !iblockstate.isFullCube())
                    {
                        list2.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound)null));
                    }
                    else
                    {
                        list.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound)null));
                    }
                }
            }

            this.blocks.clear();
            this.blocks.addAll(list);
            this.blocks.addAll(list1);
            this.blocks.addAll(list2);

            if (takeEntities)
            {
                this.takeEntitiesFromWorld(worldIn, blockpos1, blockpos2.add(1, 1, 1));
            }
            else
            {
                this.entities.clear();
            }
        }
    }

    /**
     * takes blocks from the world and puts the data them into this template
     */
    private void takeEntitiesFromWorld(World worldIn, BlockPos startPos, BlockPos endPos)
    {
        List<Entity> list = worldIn.<Entity>getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), new Predicate<Entity>()
        {
            public boolean apply(@Nullable Entity p_apply_1_)
            {
                return !(p_apply_1_ instanceof EntityPlayer);
            }
        });
        this.entities.clear();

        for (Entity entity : list)
        {
            Vec3d vec3d = new Vec3d(entity.posX - (double)startPos.getX(), entity.posY - (double)startPos.getY(), entity.posZ - (double)startPos.getZ());
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            entity.writeToNBTOptional(nbttagcompound);
            BlockPos blockpos;

            if (entity instanceof EntityPainting)
            {
                blockpos = ((EntityPainting)entity).getHangingPosition().subtract(startPos);
            }
            else
            {
                blockpos = new BlockPos(vec3d);
            }

            this.entities.add(new Template.EntityInfo(vec3d, blockpos, nbttagcompound));
        }
    }

    public Map<BlockPos, String> getDataBlocks(BlockPos pos, CapsulePlacementSettings placementIn)
    {
        Map<BlockPos, String> map = Maps.<BlockPos, String>newHashMap();
        StructureBoundingBox structureboundingbox = placementIn.getBoundingBox();

        for (Template.BlockInfo template$blockinfo : this.blocks)
        {
            BlockPos blockpos = transformedBlockPos(placementIn, template$blockinfo.pos).add(pos);

            if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos))
            {
                IBlockState iblockstate = template$blockinfo.blockState;

                if (iblockstate.getBlock() == Blocks.STRUCTURE_BLOCK && template$blockinfo.tileentityData != null)
                {
                    TileEntityStructure.Mode tileentitystructure$mode = TileEntityStructure.Mode.valueOf(template$blockinfo.tileentityData.getString("mode"));

                    if (tileentitystructure$mode == TileEntityStructure.Mode.DATA)
                    {
                        map.put(blockpos, template$blockinfo.tileentityData.getString("metadata"));
                    }
                }
            }
        }

        return map;
    }

    public BlockPos calculateConnectedPos(CapsulePlacementSettings placementIn, BlockPos p_186262_2_, CapsulePlacementSettings p_186262_3_, BlockPos p_186262_4_)
    {
        BlockPos blockpos = transformedBlockPos(placementIn, p_186262_2_);
        BlockPos blockpos1 = transformedBlockPos(p_186262_3_, p_186262_4_);
        return blockpos.subtract(blockpos1);
    }

    public static BlockPos transformedBlockPos(CapsulePlacementSettings placementIn, BlockPos pos)
    {
        return transformedBlockPos(pos, placementIn.getMirror(), placementIn.getRotation());
    }

    /**
     * Add blocks and entities from this structure to the given world, restricting placement to within the chunk
     * bounding box.
     *
     * @see CapsulePlacementSettings#setBoundingBoxFromChunk
     *
     * @param worldIn The world to use
     * @param pos The origin position for the structure
     */
    public void addBlocksToWorldChunk(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn)
    {
        placementIn.setBoundingBoxFromChunk();
        this.addBlocksToWorld(worldIn, pos, placementIn);
    }

    /**
     * This takes the data stored in this instance and puts them into the world.
     *
     * @param worldIn The world to use
     * @param pos The origin position for the structure
     * @param placementIn Placement settings to use
     */
    public void addBlocksToWorld(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn)
    {
        this.addBlocksToWorld(worldIn, pos, new BlockRotationProcessor(pos, placementIn), placementIn, 2);
    }

    /**
     * Adds blocks and entities from this structure to the given world.
     *
     * @param worldIn The world to use
     * @param pos The origin position for the structure
     * @param placementIn Placement settings to use
     * @param flags Flags to pass to {@link World#setBlockState(BlockPos, IBlockState, int)}
     */
    public void addBlocksToWorld(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn, int flags)
    {
        this.addBlocksToWorld(worldIn, pos, new BlockRotationProcessor(pos, placementIn), placementIn, flags);
    }

    /**
     * Adds blocks and entities from this structure to the given world.
     *
     * @param worldIn The world to use
     * @param pos The origin position for the structure
     * @param templateProcessor The template processor to use
     * @param placementIn Placement settings to use
     * @param flags Flags to pass to {@link World#setBlockState(BlockPos, IBlockState, int)}
     */
    public void addBlocksToWorld(World worldIn, BlockPos pos, @Nullable ITemplateProcessor templateProcessor, CapsulePlacementSettings placementIn, int flags)
    {
        if ((!this.blocks.isEmpty() || !placementIn.getIgnoreEntities() && !this.entities.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1)
        {
            Block block = placementIn.getReplacedBlock();
            StructureBoundingBox structureboundingbox = placementIn.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : this.blocks)
            {
                BlockPos blockpos = transformedBlockPos(placementIn, template$blockinfo.pos).add(pos);
                Template.BlockInfo template$blockinfo1 = templateProcessor != null ? templateProcessor.processBlock(worldIn, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null)
                {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if ((block == null || block != block1) && (!placementIn.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) && (structureboundingbox == null || structureboundingbox.isVecInside(blockpos)))
                    {
                        IBlockState iblockstate = template$blockinfo1.blockState.withMirror(placementIn.getMirror());
                        IBlockState iblockstate1 = iblockstate.withRotation(placementIn.getRotation());

                        if (template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity = worldIn.getTileEntity(blockpos);

                            if (tileentity != null)
                            {
                                if (tileentity instanceof IInventory)
                                {
                                    ((IInventory)tileentity).clear();
                                }

                                worldIn.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (worldIn.setBlockState(blockpos, iblockstate1, flags) && template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity2 = worldIn.getTileEntity(blockpos);

                            if (tileentity2 != null)
                            {
                                template$blockinfo1.tileentityData.setInteger("x", blockpos.getX());
                                template$blockinfo1.tileentityData.setInteger("y", blockpos.getY());
                                template$blockinfo1.tileentityData.setInteger("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                tileentity2.mirror(placementIn.getMirror());
                                tileentity2.rotate(placementIn.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : this.blocks)
            {
                if (block == null || block != template$blockinfo2.blockState.getBlock())
                {
                    BlockPos blockpos1 = transformedBlockPos(placementIn, template$blockinfo2.pos).add(pos);

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1))
                    {
                        worldIn.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock(), false);

                        if (template$blockinfo2.tileentityData != null)
                        {
                            TileEntity tileentity1 = worldIn.getTileEntity(blockpos1);

                            if (tileentity1 != null)
                            {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }

            if (!placementIn.getIgnoreEntities())
            {
                this.addEntitiesToWorld(worldIn, pos, placementIn.getMirror(), placementIn.getRotation(), structureboundingbox, null);
            }
        }
    }

    private void addEntitiesToWorld(World worldIn, BlockPos pos, Mirror mirrorIn, Rotation rotationIn, @Nullable StructureBoundingBox aabb, List<Entity> spawnedEntities)
    {
        for (Template.EntityInfo template$entityinfo : this.entities)
        {
            BlockPos blockpos = transformedBlockPos(template$entityinfo.blockPos, mirrorIn, rotationIn).add(pos);

            if (aabb == null || aabb.isVecInside(blockpos))
            {
                NBTTagCompound nbttagcompound = template$entityinfo.entityData;
                Vec3d vec3d = transformedVec3d(template$entityinfo.pos, mirrorIn, rotationIn);
                Vec3d vec3d1 = vec3d.addVector((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                NBTTagList nbttaglist = new NBTTagList();
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.x));
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.y));
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.z));
                nbttagcompound.setTag("Pos", nbttaglist);
                nbttagcompound.setUniqueId("UUID", UUID.randomUUID());
                Entity entity;

                try
                {
                    entity = EntityList.createEntityFromNBT(nbttagcompound, worldIn);
                }
                catch (Exception var15)
                {
                    entity = null;
                }

                if (entity != null)
                {
                    float f = entity.getMirroredYaw(mirrorIn);
                    f = f + (entity.rotationYaw - entity.getRotatedYaw(rotationIn));
                    entity.setLocationAndAngles(vec3d1.x, vec3d1.y, vec3d1.z, f, entity.rotationPitch);
                    worldIn.spawnEntity(entity);
                    if(spawnedEntities != null) spawnedEntities.add(entity);
                }
            }
        }
    }

    public BlockPos transformedSize(Rotation rotationIn)
    {
        switch (rotationIn)
        {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new BlockPos(this.size.getZ(), this.size.getY(), this.size.getX());
            default:
                return this.size;
        }
    }

    private static BlockPos transformedBlockPos(BlockPos pos, Mirror mirrorIn, Rotation rotationIn)
    {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean flag = true;

        switch (mirrorIn)
        {
            case LEFT_RIGHT:
                k = -k;
                break;
            case FRONT_BACK:
                i = -i;
                break;
            default:
                flag = false;
        }

        switch (rotationIn)
        {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(k, j, -i);
            case CLOCKWISE_90:
                return new BlockPos(-k, j, i);
            case CLOCKWISE_180:
                return new BlockPos(-i, j, -k);
            default:
                return flag ? new BlockPos(i, j, k) : pos;
        }
    }

    private static Vec3d transformedVec3d(Vec3d vec, Mirror mirrorIn, Rotation rotationIn)
    {
        double d0 = vec.x;
        double d1 = vec.y;
        double d2 = vec.z;
        boolean flag = true;

        switch (mirrorIn)
        {
            case LEFT_RIGHT:
                d2 = 1.0D - d2;
                break;
            case FRONT_BACK:
                d0 = 1.0D - d0;
                break;
            default:
                flag = false;
        }

        switch (rotationIn)
        {
            case COUNTERCLOCKWISE_90:
                return new Vec3d(d2, d1, 1.0D - d0);
            case CLOCKWISE_90:
                return new Vec3d(1.0D - d2, d1, d0);
            case CLOCKWISE_180:
                return new Vec3d(1.0D - d0, d1, 1.0D - d2);
            default:
                return flag ? new Vec3d(d0, d1, d2) : vec;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos p_189961_1_, Mirror p_189961_2_, Rotation p_189961_3_)
    {
        return getZeroPositionWithTransform(p_189961_1_, p_189961_2_, p_189961_3_, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos p_191157_0_, Mirror p_191157_1_, Rotation p_191157_2_, int p_191157_3_, int p_191157_4_)
    {
        --p_191157_3_;
        --p_191157_4_;
        int i = p_191157_1_ == Mirror.FRONT_BACK ? p_191157_3_ : 0;
        int j = p_191157_1_ == Mirror.LEFT_RIGHT ? p_191157_4_ : 0;
        BlockPos blockpos = p_191157_0_;

        switch (p_191157_2_)
        {
            case COUNTERCLOCKWISE_90:
                blockpos = p_191157_0_.add(j, 0, p_191157_3_ - i);
                break;
            case CLOCKWISE_90:
                blockpos = p_191157_0_.add(p_191157_4_ - j, 0, i);
                break;
            case CLOCKWISE_180:
                blockpos = p_191157_0_.add(p_191157_3_ - i, 0, p_191157_4_ - j);
                break;
            case NONE:
                blockpos = p_191157_0_.add(i, 0, j);
        }

        return blockpos;
    }

    public static void registerFixes(DataFixer fixer)
    {
        fixer.registerWalker(FixTypes.STRUCTURE, new IDataWalker()
        {
            public NBTTagCompound process(IDataFixer fixer, NBTTagCompound compound, int versionIn)
            {
                if (compound.hasKey("entities", 9))
                {
                    NBTTagList nbttaglist = compound.getTagList("entities", 10);

                    for (int i = 0; i < nbttaglist.tagCount(); ++i)
                    {
                        NBTTagCompound nbttagcompound = (NBTTagCompound)nbttaglist.get(i);

                        if (nbttagcompound.hasKey("nbt", 10))
                        {
                            nbttagcompound.setTag("nbt", fixer.process(FixTypes.ENTITY, nbttagcompound.getCompoundTag("nbt"), versionIn));
                        }
                    }
                }

                if (compound.hasKey("blocks", 9))
                {
                    NBTTagList nbttaglist1 = compound.getTagList("blocks", 10);

                    for (int j = 0; j < nbttaglist1.tagCount(); ++j)
                    {
                        NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist1.get(j);

                        if (nbttagcompound1.hasKey("nbt", 10))
                        {
                            nbttagcompound1.setTag("nbt", fixer.process(FixTypes.BLOCK_ENTITY, nbttagcompound1.getCompoundTag("nbt"), versionIn));
                        }
                    }
                }

                return compound;
            }
        });
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
    	CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        NBTTagList nbttaglist = new NBTTagList();

        for (Template.BlockInfo template$blockinfo : this.blocks)
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setTag("pos", this.writeInts(template$blockinfo.pos.getX(), template$blockinfo.pos.getY(), template$blockinfo.pos.getZ()));
            nbttagcompound.setInteger("state", template$basicpalette.idFor(template$blockinfo.blockState));

            if (template$blockinfo.tileentityData != null)
            {
                nbttagcompound.setTag("nbt", template$blockinfo.tileentityData);
            }

            nbttaglist.appendTag(nbttagcompound);
        }

        NBTTagList nbttaglist1 = new NBTTagList();

        for (Template.EntityInfo template$entityinfo : this.entities)
        {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.setTag("pos", this.writeDoubles(template$entityinfo.pos.x, template$entityinfo.pos.y, template$entityinfo.pos.z));
            nbttagcompound1.setTag("blockPos", this.writeInts(template$entityinfo.blockPos.getX(), template$entityinfo.blockPos.getY(), template$entityinfo.blockPos.getZ()));

            if (template$entityinfo.entityData != null)
            {
                nbttagcompound1.setTag("nbt", template$entityinfo.entityData);
            }

            nbttaglist1.appendTag(nbttagcompound1);
        }

        NBTTagList nbttaglist2 = new NBTTagList();

        for (IBlockState iblockstate : template$basicpalette)
        {
            nbttaglist2.appendTag(NBTUtil.writeBlockState(new NBTTagCompound(), iblockstate));
        }

        net.minecraftforge.fml.common.FMLCommonHandler.instance().getDataFixer().writeVersionData(nbt); //Moved up for MC updating reasons.
        nbt.setTag("palette", nbttaglist2);
        nbt.setTag("blocks", nbttaglist);
        nbt.setTag("entities", nbttaglist1);
        nbt.setTag("size", this.writeInts(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.setString("author", this.author);
        nbt.setInteger("DataVersion", 1343);
        return nbt;
    }

    public void read(NBTTagCompound compound)
    {
        this.blocks.clear();
        this.entities.clear();
        NBTTagList nbttaglist = compound.getTagList("size", 3);
        this.size = new BlockPos(nbttaglist.getIntAt(0), nbttaglist.getIntAt(1), nbttaglist.getIntAt(2));
        this.author = compound.getString("author");
        CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        NBTTagList nbttaglist1 = compound.getTagList("palette", 10);

        for (int i = 0; i < nbttaglist1.tagCount(); ++i)
        {
            template$basicpalette.addMapping(NBTUtil.readBlockState(nbttaglist1.getCompoundTagAt(i)), i);
        }

        NBTTagList nbttaglist3 = compound.getTagList("blocks", 10);

        for (int j = 0; j < nbttaglist3.tagCount(); ++j)
        {
            NBTTagCompound nbttagcompound = nbttaglist3.getCompoundTagAt(j);
            NBTTagList nbttaglist2 = nbttagcompound.getTagList("pos", 3);
            BlockPos blockpos = new BlockPos(nbttaglist2.getIntAt(0), nbttaglist2.getIntAt(1), nbttaglist2.getIntAt(2));
            IBlockState iblockstate = template$basicpalette.stateFor(nbttagcompound.getInteger("state"));
            NBTTagCompound nbttagcompound1;

            if (nbttagcompound.hasKey("nbt"))
            {
                nbttagcompound1 = nbttagcompound.getCompoundTag("nbt");
            }
            else
            {
                nbttagcompound1 = null;
            }

            this.blocks.add(new Template.BlockInfo(blockpos, iblockstate, nbttagcompound1));
        }

        NBTTagList nbttaglist4 = compound.getTagList("entities", 10);

        for (int k = 0; k < nbttaglist4.tagCount(); ++k)
        {
            NBTTagCompound nbttagcompound3 = nbttaglist4.getCompoundTagAt(k);
            NBTTagList nbttaglist5 = nbttagcompound3.getTagList("pos", 6);
            Vec3d vec3d = new Vec3d(nbttaglist5.getDoubleAt(0), nbttaglist5.getDoubleAt(1), nbttaglist5.getDoubleAt(2));
            NBTTagList nbttaglist6 = nbttagcompound3.getTagList("blockPos", 3);
            BlockPos blockpos1 = new BlockPos(nbttaglist6.getIntAt(0), nbttaglist6.getIntAt(1), nbttaglist6.getIntAt(2));

            if (nbttagcompound3.hasKey("nbt"))
            {
                NBTTagCompound nbttagcompound2 = nbttagcompound3.getCompoundTag("nbt");
                this.entities.add(new Template.EntityInfo(vec3d, blockpos1, nbttagcompound2));
            }
        }
    }

    private NBTTagList writeInts(int... values)
    {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i : values)
        {
            nbttaglist.appendTag(new NBTTagInt(i));
        }

        return nbttaglist;
    }

    private NBTTagList writeDoubles(double... values)
    {
        NBTTagList nbttaglist = new NBTTagList();

        for (double d0 : values)
        {
            nbttaglist.appendTag(new NBTTagDouble(d0));
        }

        return nbttaglist;
    }

    static class BasicPalette implements Iterable<IBlockState>
        {
            public static final IBlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();
            final ObjectIntIdentityMap<IBlockState> ids;
            private int lastId;

            private BasicPalette()
            {
                this.ids = new ObjectIntIdentityMap<IBlockState>(16);
            }

            public int idFor(IBlockState state)
            {
                int i = this.ids.get(state);

                if (i == -1)
                {
                    i = this.lastId++;
                    this.ids.put(state, i);
                }

                return i;
            }

            @Nullable
            public IBlockState stateFor(int id)
            {
                IBlockState iblockstate = this.ids.getByValue(id);
                return iblockstate == null ? DEFAULT_BLOCK_STATE : iblockstate;
            }

            public Iterator<IBlockState> iterator()
            {
                return this.ids.iterator();
            }

            public void addMapping(IBlockState p_189956_1_, int p_189956_2_)
            {
                this.ids.put(p_189956_1_, p_189956_2_);
            }
        }




    // CAPSULE additions

    /**
	 * takes blocks from the world and puts the data them into this template
	 */
	public List<BlockPos> takeBlocksFromWorldIntoCapsule(World worldIn, BlockPos startPos, BlockPos endPos,
			Map<BlockPos, Block> sourceIgnorePos, List<Block> excluded, List<Entity> outCapturedEntities) {

		List<BlockPos> transferedBlocks = new ArrayList<BlockPos>();

		if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1) {
			BlockPos blockpos = startPos.add(endPos).add(-1, -1, -1);
			List<Template.BlockInfo> list = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list1 = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list2 = Lists.<Template.BlockInfo> newArrayList();
			BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()),
					Math.min(startPos.getZ(), blockpos.getZ()));
			BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()),
					Math.max(startPos.getZ(), blockpos.getZ()));
			this.size = endPos;

			for (BlockPos.MutableBlockPos blockpos$mutableblockpos : BlockPos.getAllInBoxMutable(blockpos1, blockpos2)) {
				BlockPos blockpos3 = blockpos$mutableblockpos.subtract(blockpos1);
				IBlockState iblockstate = worldIn.getBlockState(blockpos$mutableblockpos);
				Block iblock = iblockstate.getBlock();

				if (!excluded.contains(iblock) // excluded blocks are not
												// captured at all
						&& (sourceIgnorePos == null // exclude sourceBlock that
													// were already presents.
													// Capture if it was
													// changed.
								|| !(sourceIgnorePos.keySet().contains(blockpos$mutableblockpos)
										&& sourceIgnorePos.get(blockpos$mutableblockpos).equals(iblock)))) {
					TileEntity tileentity = worldIn.getTileEntity(blockpos$mutableblockpos);

					if (tileentity != null) {
						NBTTagCompound nbttagcompound = tileentity.writeToNBT(new NBTTagCompound());
						nbttagcompound.removeTag("x");
						nbttagcompound.removeTag("y");
						nbttagcompound.removeTag("z");
						list1.add(new Template.BlockInfo(blockpos3, iblockstate, nbttagcompound));
					} else if (!iblockstate.isFullBlock() && !iblockstate.isFullCube()) {
						list2.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound) null));
					} else {
						list.add(new Template.BlockInfo(blockpos3, iblockstate, (NBTTagCompound) null));
					}
					transferedBlocks.add(new BlockPos(blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ())); // save
																					// a
																					// copy
				}
			}

			blocks.clear();
			blocks.addAll(list);
			blocks.addAll(list1);
			blocks.addAll(list2);

			List<Entity> capturedEntities = this.takeNonLivingEntitiesFromWorld( worldIn, blockpos1, blockpos2.add(1, 1, 1));
			if(outCapturedEntities != null && capturedEntities != null){
				outCapturedEntities.addAll(capturedEntities);
			}

			return transferedBlocks;
		}

		return null;
	}

	/**
	 * takes blocks from the world and puts the data them into this template
	 */
	public List<Entity> takeNonLivingEntitiesFromWorld(World worldIn, BlockPos startPos, BlockPos endPos) {

		// rewritten vanilla code from CapsuleTemplate.takeEntitiesFromWorld
		List<Entity> list = worldIn.getEntitiesWithinAABB(
		        Entity.class,
                new AxisAlignedBB(startPos, endPos),
                entity -> !(entity instanceof EntityItem) && (!(entity instanceof EntityLivingBase) || (entity instanceof EntityArmorStand))
        );
		entities.clear();

		for (Entity entity : list) {
			Vec3d vec3d = new Vec3d(entity.posX - (double) startPos.getX(), entity.posY - (double) startPos.getY(), entity.posZ - (double) startPos.getZ());
			NBTTagCompound nbttagcompound = new NBTTagCompound();
			entity.writeToNBTOptional(nbttagcompound);
			BlockPos blockpos;

			if (entity instanceof EntityPainting) {
				blockpos = ((EntityPainting) entity).getHangingPosition().subtract(startPos);
			} else {
				blockpos = new BlockPos(vec3d);
			}

			entities.add(new Template.EntityInfo(vec3d, blockpos, nbttagcompound));
		}

		return list;
	}

    /**
     * Tweaked version of "addBlocksToWorld" for capsule
     */
	public void spawnBlocksAndEntities(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn, List<BlockPos> spawnedBlocks, List<Entity> spawnedEntities)
    {
        int flags = 2;
		ITemplateProcessor templateProcessor = new BlockRotationProcessor(pos, placementIn);

		if(blocks == null || size == null || templateProcessor == null) return;


        if (!this.blocks.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1)
        {
            Block block = placementIn.getReplacedBlock();
            StructureBoundingBox structureboundingbox = placementIn.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : this.blocks)
            {
                BlockPos blockpos = transformedBlockPos(placementIn, template$blockinfo.pos).add(pos);
                Template.BlockInfo template$blockinfo1 = templateProcessor != null ? templateProcessor.processBlock(worldIn, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null)
                {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if ((block == null || block != block1) && (!placementIn.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) && (structureboundingbox == null || structureboundingbox.isVecInside(blockpos)))
                    {
                        // CAPSULE capsule addition to allow a rollback in case of error while deploying
                    	if(spawnedBlocks != null) spawnedBlocks.add(blockpos);

                        IBlockState iblockstate = template$blockinfo1.blockState.withMirror(placementIn.getMirror());
                        IBlockState iblockstate1 = iblockstate.withRotation(placementIn.getRotation());

                        if (template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity = worldIn.getTileEntity(blockpos);

                            if (tileentity != null)
                            {
                                if (tileentity instanceof IInventory)
                                {
                                    ((IInventory)tileentity).clear();
                                }

                                worldIn.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (worldIn.setBlockState(blockpos, iblockstate1, 2) && template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity2 = worldIn.getTileEntity(blockpos);

                            if (tileentity2 != null)
                            {
                                template$blockinfo1.tileentityData.setInteger("x", blockpos.getX());
                                template$blockinfo1.tileentityData.setInteger("y", blockpos.getY());
                                template$blockinfo1.tileentityData.setInteger("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                tileentity2.mirror(placementIn.getMirror());
                                tileentity2.rotate(placementIn.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : this.blocks)
            {
                if (block == null || block != template$blockinfo2.blockState.getBlock())
                {
                    BlockPos blockpos1 = transformedBlockPos(placementIn, template$blockinfo2.pos).add(pos);

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1))
                    {
                        worldIn.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock(), false);

                        if (template$blockinfo2.tileentityData != null)
                        {
                            TileEntity tileentity1 = worldIn.getTileEntity(blockpos1);

                            if (tileentity1 != null)
                            {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }

            if (!placementIn.getIgnoreEntities())
            {
                this.addEntitiesToWorld(worldIn, pos, placementIn.getMirror(), placementIn.getRotation(), structureboundingbox, null);
            }
        }
    }


    public void removeBlocks(List<BlockPos> couldNotBeRemoved, BlockPos startPos) {
        for (BlockPos blockPos : couldNotBeRemoved) {
            this.blocks.removeIf(blockInfo -> blockPos.subtract(startPos).equals(blockInfo.pos));
        }
    }

    /**
     * list positions of futur deployment
     */
    public List<BlockPos> calculateDeployPositions(World world, BlockPos blockPos, CapsulePlacementSettings CapsulePlacementSettings) {

        ArrayList<BlockPos> out = new ArrayList<>();
        ITemplateProcessor blockRotationProcessor = new BlockRotationProcessor(blockPos, CapsulePlacementSettings);
        if (blocks == null || size == null || blockRotationProcessor == null) return out;

        if (!this.blocks.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
            Block block = CapsulePlacementSettings.getReplacedBlock();
            StructureBoundingBox structureboundingbox = CapsulePlacementSettings.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : this.blocks) {
                BlockPos blockpos = transformedBlockPos(CapsulePlacementSettings, template$blockinfo.pos).add(blockPos);
                Template.BlockInfo template$blockinfo1 = blockRotationProcessor != null ? blockRotationProcessor.processBlock(world, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null) {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if (
                            (block == null || block != block1) &&
                                    (!CapsulePlacementSettings.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) &&
                                    (structureboundingbox == null || structureboundingbox.isVecInside(blockpos))
                            ) {
                        out.add(blockpos);
                    }
                }
            }
        }
        return out;
    }

}