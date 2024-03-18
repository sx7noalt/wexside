/*
 * Decompiled with CFR 0.150.
 */
package net.minecraft.world.chunk;

import baritone.utils.accessor.IBlockStateContainer;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import net.minecraft.world.chunk.BlockStatePaletteRegistry;
import net.minecraft.world.chunk.IBlockStatePalette;
import net.minecraft.world.chunk.IBlockStatePaletteResizer;
import net.minecraft.world.chunk.NibbleArray;

public class BlockStateContainer
        implements IBlockStatePaletteResizer,
        IBlockStateContainer {
    private static final IBlockStatePalette REGISTRY_BASED_PALETTE = new BlockStatePaletteRegistry();
    protected static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected BitArray storage;
    protected IBlockStatePalette palette;
    private int bits;

    public BlockStateContainer() {
        this.setBits(4);
    }

    private static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private void setBits(int bitsIn) {
        if (bitsIn != this.bits) {
            this.bits = bitsIn;
            if (this.bits <= 4) {
                this.bits = 4;
                this.palette = new BlockStatePaletteLinear(this.bits, this);
            } else if (this.bits <= 8) {
                this.palette = new BlockStatePaletteHashMap(this.bits, this);
            } else {
                this.palette = REGISTRY_BASED_PALETTE;
                this.bits = MathHelper.log2DeBruijn(Block.BLOCK_STATE_IDS.size());
            }
            this.palette.idFor(AIR_BLOCK_STATE);
            this.storage = new BitArray(this.bits, 4096);
        }
    }

    @Override
    public int onResize(int p_186008_1_, IBlockState state) {
        BitArray bitarray = this.storage;
        IBlockStatePalette iblockstatepalette = this.palette;
        this.setBits(p_186008_1_);
        for (int i = 0; i < bitarray.size(); ++i) {
            IBlockState iblockstate = iblockstatepalette.getBlockState(bitarray.getAt(i));
            if (iblockstate == null) continue;
            this.set(i, iblockstate);
        }
        return this.palette.idFor(state);
    }

    public void set(int x, int y, int z, IBlockState state) {
        this.set(BlockStateContainer.getIndex(x, y, z), state);
    }

    protected void set(int index, IBlockState state) {
        int i = this.palette.idFor(state);
        this.storage.setAt(index, i);
    }

    public IBlockState get(int x, int y, int z) {
        return this.get(BlockStateContainer.getIndex(x, y, z));
    }

    protected IBlockState get(int index) {
        IBlockState iblockstate = this.palette.getBlockState(this.storage.getAt(index));
        return iblockstate == null ? AIR_BLOCK_STATE : iblockstate;
    }

    public void read(PacketBuffer buf) {
        byte i = buf.readByte();
        if (this.bits != i) {
            this.setBits(i);
        }
        this.palette.read(buf);
        buf.readLongArray(this.storage.getBackingLongArray());
    }

    public void write(PacketBuffer buf) {
        buf.writeByte(this.bits);
        this.palette.write(buf);
        buf.writeLongArray(this.storage.getBackingLongArray());
    }

    @Nullable
    public NibbleArray getDataForNBT(byte[] p_186017_1_, NibbleArray p_186017_2_) {
        NibbleArray nibblearray = null;
        for (int i = 0; i < 4096; ++i) {
            int j = Block.BLOCK_STATE_IDS.get(this.get(i));
            int k = i & 0xF;
            int l = i >> 8 & 0xF;
            int i1 = i >> 4 & 0xF;
            if ((j >> 12 & 0xF) != 0) {
                if (nibblearray == null) {
                    nibblearray = new NibbleArray();
                }
                nibblearray.set(k, l, i1, j >> 12 & 0xF);
            }
            p_186017_1_[i] = (byte)(j >> 4 & 0xFF);
            p_186017_2_.set(k, l, i1, j & 0xF);
        }
        return nibblearray;
    }

    public void setDataFromNBT(byte[] p_186019_1_, NibbleArray p_186019_2_, @Nullable NibbleArray p_186019_3_) {
        for (int i = 0; i < 4096; ++i) {
            int j = i & 0xF;
            int k = i >> 8 & 0xF;
            int l = i >> 4 & 0xF;
            int i1 = p_186019_3_ == null ? 0 : p_186019_3_.get(j, k, l);
            int j1 = i1 << 12 | (p_186019_1_[i] & 0xFF) << 4 | p_186019_2_.get(j, k, l);
            this.set(i, Block.BLOCK_STATE_IDS.getByValue(j1));
        }
    }

    public int getSerializedSize() {
        return 1 + this.palette.getSerializedState() + PacketBuffer.getVarIntSize(this.storage.size()) + this.storage.getBackingLongArray().length * 8;
    }

    @Override
    public IBlockState getAtPalette(int index) {
        return this.palette.getBlockState(index);
    }

    @Override
    public int[] storageArray() {
        return this.storage.toArray();
    }
}

