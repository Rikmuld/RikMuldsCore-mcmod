package com.rikmuld.corerm.objs.blocks

import com.rikmuld.corerm.tileentity.TileEntityBounds
import com.rikmuld.corerm.utils.BlockData
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}
import net.minecraft.world.World

object Bounds {
  def readBoundsToNBT(tag: NBTTagCompound): Bounds = {
    new Bounds(tag.getFloat("xMin"), tag.getFloat("yMin"), tag.getFloat("zMin"), tag.getFloat("xMax"), tag.getFloat("yMax"), tag.getFloat("zMax"))
  }
}

class Bounds(var xMin: Float, var yMin: Float, var zMin: Float, var xMax: Float, var yMax: Float, var zMax: Float) {
  def getBlockBounds:AxisAlignedBB = 
    new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax)
  def getBlockCollision:AxisAlignedBB =
    new AxisAlignedBB(Math.max(xMin, 0), Math.max(yMin, 0), Math.max(zMin, 0), Math.min(xMax, 1), Math.min(yMax, 1), Math.min(zMax, 1))
   
  def writeBoundsToNBT(tag: NBTTagCompound) {
    tag.setFloat("xMin", xMin)
    tag.setFloat("yMin", yMin)
    tag.setFloat("zMin", zMin)
    tag.setFloat("xMax", xMax)
    tag.setFloat("yMax", yMax)
    tag.setFloat("zMax", zMax)
  }
  override def toString() = super.toString + ":" + xMin + ":" + yMin + ":" + zMin + ":" + xMax + ":" + yMax + ":" + zMax;
}

object BoundsStructure {
  def regsisterStructure(xCoords: Array[Int], yCoords: Array[Int], zCoords: Array[Int], rotation: Boolean): Array[BoundsStructure] = {
    if (!rotation) Array(new BoundsStructure(Array(xCoords, yCoords, zCoords)))
    else {
      val structure = Array.ofDim[BoundsStructure](4)
      structure(0) = new BoundsStructure(Array(xCoords, yCoords, zCoords))
      structure(1) = new BoundsStructure(Array(zCoords.map(i => -i), yCoords, xCoords.map(i => -i)))
      structure(2) = new BoundsStructure(Array(xCoords.map(i => -i), yCoords, zCoords.map(i => -i)))
      structure(3) = new BoundsStructure(Array(zCoords, yCoords, xCoords))
      structure
    }
  }
}

class BoundsStructure(var blocks: Array[Array[Int]]) {
  def canBePlaced(world: World, tracker: BoundsTracker): Boolean = {
    for (i <- blocks(0).indices if !BlockData(world, getPos(world, tracker, i)).isReplaceable) {
       return false
    }
    hadSolidUnderGround(world, tracker)
  }
  def createStructure(world: World, tracker: BoundsTracker, boundsBlockState:IBlockState) {
    if(boundsBlockState.getBlock.isInstanceOf[IBoundsBlock]){
      for (i <- blocks(0).indices) {
        val bd = getBlockData(world, tracker, i)
        bd.setState(boundsBlockState)
        bd.tile.asInstanceOf[TileEntityBounds].setBounds(tracker.getBoundsOnRelativePoistion(blocks(0)(i), blocks(1)(i), blocks(2)(i)))
        bd.tile.asInstanceOf[TileEntityBounds].setBaseCoords(tracker.baseX, tracker.baseY, tracker.baseZ)
      }
    }
  }

  def getPos(world:World, tracker:BoundsTracker, index:Int):BlockPos =
    new BlockPos(tracker.baseX + blocks(0)(index), tracker.baseY + blocks(1)(index), tracker.baseZ + blocks(2)(index))

  def getBlockData(world:World, tracker:BoundsTracker, index:Int): BlockData =
    BlockData(world, getPos(world, tracker, index))

  def destroyStructure(world: World, tracker: BoundsTracker) =
    for (i <- blocks(0).indices)
      getBlockData(world, tracker, i).toAir

  def hadSolidUnderGround(world: World, tracker: BoundsTracker): Boolean = {
    (0 until blocks(0).length).find(i => !world.isSideSolid(new BlockPos(tracker.baseX + blocks(0)(i), tracker.baseY - 1, tracker.baseZ + blocks(2)(i)), EnumFacing.UP)).map(_ => false).getOrElse(true)
  }
}

class BoundsTracker(var baseX: Int, var baseY: Int, var baseZ: Int, var bounds: Bounds) {
  def getBoundsOnRelativePoistion(xDiv: Int, yDiv: Int, zDiv: Int): Bounds = new Bounds(bounds.xMin - xDiv, bounds.yMin - yDiv, bounds.zMin - zDiv, bounds.xMax - xDiv, bounds.yMax - yDiv, bounds.zMax - zDiv)
}