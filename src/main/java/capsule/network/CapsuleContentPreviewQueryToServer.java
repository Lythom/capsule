package capsule.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * This Network Message is sent from the client to the server
 */
public class CapsuleContentPreviewQueryToServer implements IMessage {

	private String structureName = null;

	public CapsuleContentPreviewQueryToServer(String structureName) {
		this.setStructureName(structureName);
	}

	// for use by the message handler only.
	public CapsuleContentPreviewQueryToServer() {
		
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
			this.setStructureName(ByteBufUtils.readUTF8String(buf));
			
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
		ByteBufUtils.writeUTF8String(buf, this.getStructureName());
	}
	
	public boolean isMessageValid(){
		return this.getStructureName() != null;
	}

	@Override
	public String toString() {
		return "CapsuleContentPreviewMessageToClient";
	}

	public String getStructureName() {
		return structureName;
	}

	public void setStructureName(String structureName) {
		this.structureName = structureName;
	}

}