package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleThrowQueryToServer implements IMessage {

	private BlockPos pos = null;

	public CapsuleThrowQueryToServer(BlockPos pos) {
		setPos(pos);
	}

	// for use by the message handler only.
	public CapsuleThrowQueryToServer() {
		
	}

	/**
	 * Called by the network code once it has received the message bytes over
	 * the network. Used to read the ByteBuf contents into your member variables
	 * 
	 * @param buf
	 */
	@Override
	public void fromBytes(ByteBuf buf) {
		try {
			// these methods may also be of use for your code:
			// for Itemstacks - ByteBufUtils.readItemStack()
			// for NBT tags ByteBufUtils.readTag();
			// for Strings: ByteBufUtils.readUTF8String();
			this.setPos(BlockPos.fromLong(buf.readLong()));
			
		} catch (IndexOutOfBoundsException ioe) {
			System.err.println("Exception while reading AskCapsuleContentPreviewMessageToServer: " + ioe);
			return;
		}
	}

	/**
	 * Called by the network code. Used to write the contents of your message
	 * member variables into the ByteBuf, ready for transmission over the
	 * network.
	 * 
	 * @param buf
	 */
	@Override
	public void toBytes(ByteBuf buf) {

		// these methods may also be of use for your code:
		// for Itemstacks - ByteBufUtils.writeItemStack()
		// for NBT tags ByteBufUtils.writeTag();
		// for Strings: ByteBufUtils.writeUTF8String();
		buf.writeLong(this.getPos().toLong());
	}
	
	public boolean isMessageValid(){
		return this.getPos() != null;
	}

	@Override
	public String toString() {
		return "AskCapsuleThrowToServer";
	}

	public BlockPos getPos() {
		return pos;
	}

	public void setPos(BlockPos pos) {
		this.pos = pos;
	}

}