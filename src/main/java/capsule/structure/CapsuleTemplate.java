package capsule.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityStructure;
import net.minecraft.util.Mirror;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.BlockRotationProcessor;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.Template;

/**
 * Exact copy of mc original Template class, but having fields public to allow external manipulation
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

    public static BlockPos transformedBlockPos(CapsulePlacementSettings placementIn, BlockPos p_186266_1_)
    {
        return transformedBlockPos(p_186266_1_, placementIn.getMirror(), placementIn.getRotation());
    }

    public void addBlocksToWorldChunk(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn)
    {
        placementIn.setBoundingBoxFromChunk();
        this.addBlocksToWorld(worldIn, pos, placementIn);
    }

    /**
     * This takes the data stored in this instance and puts them into the world.
     */
    public void addBlocksToWorld(World worldIn, BlockPos pos, CapsulePlacementSettings placementIn)
    {
        this.func_189960_a(worldIn, pos, new BlockRotationProcessor(pos, placementIn), placementIn, 2);
    }

    public void func_189962_a(World p_189962_1_, BlockPos p_189962_2_, CapsulePlacementSettings p_189962_3_, int p_189962_4_)
    {
        this.func_189960_a(p_189962_1_, p_189962_2_, new BlockRotationProcessor(p_189962_2_, p_189962_3_), p_189962_3_, p_189962_4_);
    }

    public void func_189960_a(World p_189960_1_, BlockPos p_189960_2_, @Nullable ITemplateProcessor p_189960_3_, CapsulePlacementSettings p_189960_4_, int p_189960_5_)
    {
        if (!this.blocks.isEmpty() && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1)
        {
            Block block = p_189960_4_.getReplacedBlock();
            StructureBoundingBox structureboundingbox = p_189960_4_.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : this.blocks)
            {
                BlockPos blockpos = transformedBlockPos(p_189960_4_, template$blockinfo.pos).add(p_189960_2_);
                Template.BlockInfo template$blockinfo1 = p_189960_3_ != null ? p_189960_3_.func_189943_a(p_189960_1_, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null)
                {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if ((block == null || block != block1) && (!p_189960_4_.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) && (structureboundingbox == null || structureboundingbox.isVecInside(blockpos)))
                    {
                        IBlockState iblockstate = template$blockinfo1.blockState.withMirror(p_189960_4_.getMirror());
                        IBlockState iblockstate1 = iblockstate.withRotation(p_189960_4_.getRotation());

                        if (template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity != null)
                            {
                                if (tileentity instanceof IInventory)
                                {
                                    ((IInventory)tileentity).clear();
                                }

                                p_189960_1_.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (p_189960_1_.setBlockState(blockpos, iblockstate1, p_189960_5_) && template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity2 = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity2 != null)
                            {
                                template$blockinfo1.tileentityData.setInteger("x", blockpos.getX());
                                template$blockinfo1.tileentityData.setInteger("y", blockpos.getY());
                                template$blockinfo1.tileentityData.setInteger("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                tileentity2.func_189668_a(p_189960_4_.getMirror());
                                tileentity2.func_189667_a(p_189960_4_.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : this.blocks)
            {
                if (block == null || block != template$blockinfo2.blockState.getBlock())
                {
                    BlockPos blockpos1 = transformedBlockPos(p_189960_4_, template$blockinfo2.pos).add(p_189960_2_);

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1))
                    {
                        p_189960_1_.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock());

                        if (template$blockinfo2.tileentityData != null)
                        {
                            TileEntity tileentity1 = p_189960_1_.getTileEntity(blockpos1);

                            if (tileentity1 != null)
                            {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }

            if (!p_189960_4_.getIgnoreEntities())
            {
                this.addEntitiesToWorld(p_189960_1_, p_189960_2_, p_189960_4_.getMirror(), p_189960_4_.getRotation(), structureboundingbox, null);
            }
        }
    }

    public void addEntitiesToWorld(World worldIn, BlockPos pos, Mirror mirrorIn, Rotation rotationIn, @Nullable StructureBoundingBox aabb, List<Entity> spawnedEntities)
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
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.xCoord));
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.yCoord));
                nbttaglist.appendTag(new NBTTagDouble(vec3d1.zCoord));
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
                    entity.setLocationAndAngles(vec3d1.xCoord, vec3d1.yCoord, vec3d1.zCoord, f, entity.rotationPitch);
                    worldIn.spawnEntityInWorld(entity);
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
        double d0 = vec.xCoord;
        double d1 = vec.yCoord;
        double d2 = vec.zCoord;
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

    public BlockPos func_189961_a(BlockPos p_189961_1_, Mirror p_189961_2_, Rotation p_189961_3_)
    {
        int i = this.getSize().getX() - 1;
        int j = this.getSize().getZ() - 1;
        int k = p_189961_2_ == Mirror.FRONT_BACK ? i : 0;
        int l = p_189961_2_ == Mirror.LEFT_RIGHT ? j : 0;
        BlockPos blockpos = p_189961_1_;

        switch (p_189961_3_)
        {
            case COUNTERCLOCKWISE_90:
                blockpos = p_189961_1_.add(l, 0, i - k);
                break;
            case CLOCKWISE_90:
                blockpos = p_189961_1_.add(j - l, 0, k);
                break;
            case CLOCKWISE_180:
                blockpos = p_189961_1_.add(i - k, 0, j - l);
                break;
            case NONE:
                blockpos = p_189961_1_.add(k, 0, l);
        }

        return blockpos;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
    	CapsuleTemplate.BasicPalette template$basicpalette = new CapsuleTemplate.BasicPalette();
        NBTTagList nbttaglist = new NBTTagList();

        for (Template.BlockInfo template$blockinfo : this.blocks)
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setTag("pos", this.writeInts(new int[] {template$blockinfo.pos.getX(), template$blockinfo.pos.getY(), template$blockinfo.pos.getZ()}));
            nbttagcompound.setInteger("state", template$basicpalette.func_189954_a(template$blockinfo.blockState));

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
            nbttagcompound1.setTag("pos", this.writeDoubles(new double[] {template$entityinfo.pos.xCoord, template$entityinfo.pos.yCoord, template$entityinfo.pos.zCoord}));
            nbttagcompound1.setTag("blockPos", this.writeInts(new int[] {template$entityinfo.blockPos.getX(), template$entityinfo.blockPos.getY(), template$entityinfo.blockPos.getZ()}));

            if (template$entityinfo.entityData != null)
            {
                nbttagcompound1.setTag("nbt", template$entityinfo.entityData);
            }

            nbttaglist1.appendTag(nbttagcompound1);
        }

        NBTTagList nbttaglist2 = new NBTTagList();

        for (IBlockState iblockstate : template$basicpalette)
        {
            nbttaglist2.appendTag(NBTUtil.func_190009_a(new NBTTagCompound(), iblockstate));
        }

        nbt.setTag("palette", nbttaglist2);
        nbt.setTag("blocks", nbttaglist);
        nbt.setTag("entities", nbttaglist1);
        nbt.setTag("size", this.writeInts(new int[] {this.size.getX(), this.size.getY(), this.size.getZ()}));
        nbt.setInteger("version", 1);
        nbt.setString("author", this.author);
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
            template$basicpalette.func_189956_a(NBTUtil.func_190008_d(nbttaglist1.getCompoundTagAt(i)), i);
        }

        NBTTagList nbttaglist3 = compound.getTagList("blocks", 10);

        for (int j = 0; j < nbttaglist3.tagCount(); ++j)
        {
            NBTTagCompound nbttagcompound = nbttaglist3.getCompoundTagAt(j);
            NBTTagList nbttaglist2 = nbttagcompound.getTagList("pos", 3);
            BlockPos blockpos = new BlockPos(nbttaglist2.getIntAt(0), nbttaglist2.getIntAt(1), nbttaglist2.getIntAt(2));
            IBlockState iblockstate = template$basicpalette.func_189955_a(nbttagcompound.getInteger("state"));
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
            public static final IBlockState field_189957_a = Blocks.AIR.getDefaultState();
            final ObjectIntIdentityMap<IBlockState> field_189958_b;
            private int field_189959_c;

            private BasicPalette()
            {
                this.field_189958_b = new ObjectIntIdentityMap<>(16);
            }

            public int func_189954_a(IBlockState p_189954_1_)
            {
                int i = this.field_189958_b.get(p_189954_1_);

                if (i == -1)
                {
                    i = this.field_189959_c++;
                    this.field_189958_b.put(p_189954_1_, i);
                }

                return i;
            }

            @Nullable
            public IBlockState func_189955_a(int p_189955_1_)
            {
                IBlockState iblockstate = (IBlockState)this.field_189958_b.getByValue(p_189955_1_);
                return iblockstate == null ? field_189957_a : iblockstate;
            }

            public Iterator<IBlockState> iterator()
            {
                return this.field_189958_b.iterator();
            }

            public void func_189956_a(IBlockState p_189956_1_, int p_189956_2_)
            {
                this.field_189958_b.put(p_189956_1_, p_189956_2_);
            }
        }
    
    
    
    
    // CAPSULE additions
    
    /**
	 * Rewritten from CapsuleTemplate.takeBlocksFromWorld
	 * takes blocks from the world and puts the data them into this template
	 */
	public List<BlockPos> takeBlocksFromWorldIntoCapsule(World worldIn, BlockPos startPos, BlockPos endPos,
			Map<BlockPos, Block> sourceIgnorePos, List<Block> excluded, List<Entity> outCapturedEntities) {

		List<BlockPos> transferedBlocks = new ArrayList<BlockPos>();

		// rewritten vanilla code from CapsuleTemplate.takeBlocksFromWorld

		if (endPos.getX() >= 1 && endPos.getY() >= 1 && endPos.getZ() >= 1) {
			BlockPos blockpos = startPos.add(endPos).add(-1, -1, -1);
			List<Template.BlockInfo> list = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list1 = Lists.<Template.BlockInfo> newArrayList();
			List<Template.BlockInfo> list2 = Lists.<Template.BlockInfo> newArrayList();
			BlockPos blockpos1 = new BlockPos(Math.min(startPos.getX(), blockpos.getX()), Math.min(startPos.getY(), blockpos.getY()),
					Math.min(startPos.getZ(), blockpos.getZ()));
			BlockPos blockpos2 = new BlockPos(Math.max(startPos.getX(), blockpos.getX()), Math.max(startPos.getY(), blockpos.getY()),
					Math.max(startPos.getZ(), blockpos.getZ()));

			// template.size = endPos;
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
		List<Entity> list = worldIn.<Entity> getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(startPos, endPos), new Predicate<Entity>() {
			public boolean apply(@Nullable Entity p_apply_1_) {
				return !(p_apply_1_ instanceof EntityItem) && (!(p_apply_1_ instanceof EntityLivingBase) || (p_apply_1_ instanceof EntityArmorStand));
			}
		});
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
	
	public void spawnBlocksAndEntities(World p_189960_1_, BlockPos p_189960_2_, CapsulePlacementSettings p_189960_4_, Map<BlockPos,Block> occupiedPositions, List<Block> overridableBlocks, List<BlockPos> spawnedBlocks, List<Entity> spawnedEntities)
    {
		
		ITemplateProcessor p_189960_3_ = new BlockRotationProcessor(p_189960_2_, p_189960_4_);
		int p_189960_5_ = 2;
		
		if(blocks == null || size == null || p_189960_3_== null) return; 
		

        if (!blocks.isEmpty() && size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1)
        {
            Block block = p_189960_4_.getReplacedBlock();
            StructureBoundingBox structureboundingbox = p_189960_4_.getBoundingBox();

            for (Template.BlockInfo template$blockinfo : blocks)
            {
                BlockPos blockpos = CapsuleTemplate.transformedBlockPos(p_189960_4_, template$blockinfo.pos).add(p_189960_2_);
                Template.BlockInfo template$blockinfo1 = p_189960_3_ != null ? p_189960_3_.func_189943_a(p_189960_1_, blockpos, template$blockinfo) : template$blockinfo;

                if (template$blockinfo1 != null)
                {
                    Block block1 = template$blockinfo1.blockState.getBlock();

                    if (
                    		(block == null || block != block1) && 
                    		(!p_189960_4_.getIgnoreStructureBlock() || block1 != Blocks.STRUCTURE_BLOCK) && 
                    		(structureboundingbox == null || structureboundingbox.isVecInside(blockpos)) &&
                    		// add a condition to prevent replacement of existing content by the capsule content if the world content is not overridable
                    		(!occupiedPositions.containsKey(blockpos) || overridableBlocks.contains(occupiedPositions.get(blockpos))) 
                    )
                    {
                    	// capsule addition to allow a rollback in case of error while deploying
                    	if(spawnedBlocks != null) spawnedBlocks.add(blockpos);
                    	
                        IBlockState iblockstate = template$blockinfo1.blockState.withMirror(p_189960_4_.getMirror());
                        IBlockState iblockstate1 = iblockstate.withRotation(p_189960_4_.getRotation());

                        if (template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity != null)
                            {
                                if (tileentity instanceof IInventory)
                                {
                                    ((IInventory)tileentity).clear();
                                }

                                p_189960_1_.setBlockState(blockpos, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (p_189960_1_.setBlockState(blockpos, iblockstate1, p_189960_5_) && template$blockinfo1.tileentityData != null)
                        {
                            TileEntity tileentity2 = p_189960_1_.getTileEntity(blockpos);

                            if (tileentity2 != null)
                            {
                                template$blockinfo1.tileentityData.setInteger("x", blockpos.getX());
                                template$blockinfo1.tileentityData.setInteger("y", blockpos.getY());
                                template$blockinfo1.tileentityData.setInteger("z", blockpos.getZ());
                                tileentity2.readFromNBT(template$blockinfo1.tileentityData);
                                
                                tileentity2.func_189668_a(p_189960_4_.getMirror());
                                tileentity2.func_189667_a(p_189960_4_.getRotation());
                            }
                        }
                    }
                }
            }

            for (Template.BlockInfo template$blockinfo2 : blocks)
            {
                if (block == null || block != template$blockinfo2.blockState.getBlock())
                {
                    BlockPos blockpos1 = CapsuleTemplate.transformedBlockPos(p_189960_4_, template$blockinfo2.pos).add(p_189960_2_);

                    if (structureboundingbox == null || structureboundingbox.isVecInside(blockpos1))
                    {
                        p_189960_1_.notifyNeighborsRespectDebug(blockpos1, template$blockinfo2.blockState.getBlock());

                        if (template$blockinfo2.tileentityData != null)
                        {
                            TileEntity tileentity1 = p_189960_1_.getTileEntity(blockpos1);

                            if (tileentity1 != null)
                            {
                                tileentity1.markDirty();
                            }
                        }
                    }
                }
            }

            if (!p_189960_4_.getIgnoreEntities())
            {
            	this.addEntitiesToWorld( p_189960_1_, p_189960_2_, p_189960_4_.getMirror(), p_189960_4_.getRotation(), structureboundingbox, spawnedEntities);
            }
        }
    }

}