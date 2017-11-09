package capsule.structure;

import com.google.common.collect.Maps;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Map;

/**
 * Exact copy of mc original net.minecraft.world.gen.structure.template.TemplateManager, but using CapsuleTemplate instead and custom jar source folder.
 * @author Lythom
 */
public class CapsuleTemplateManager
{
    protected static final Logger LOGGER = LogManager.getLogger(CapsuleTemplateManager.class);

    private final Map<String, CapsuleTemplate> templates = Maps.<String, CapsuleTemplate>newHashMap();
    /** the folder in the assets folder where the structure templates are found. */
    private final String baseFolder;

    public CapsuleTemplateManager(String basefolderIn)
    {
        this.baseFolder = basefolderIn;
    }

    public CapsuleTemplate getTemplate(@Nullable MinecraftServer server, ResourceLocation id)
    {
    	CapsuleTemplate template = this.get(server, id);

        if (template == null)
        {
            template = new CapsuleTemplate();
            this.templates.put(id.getResourcePath(), template);
        }

        return template;
    }

    @Nullable
    public CapsuleTemplate get(@Nullable MinecraftServer p_189942_1_, ResourceLocation p_189942_2_)
    {
        String s = p_189942_2_.getResourcePath();

        if (this.templates.containsKey(s))
        {
            return (CapsuleTemplate)this.templates.get(s);
        }
        else
        {
            if (p_189942_1_ != null)
            {
                this.readTemplate(p_189942_1_, p_189942_2_);
            }
            else
            {
                this.readTemplateFromJar(p_189942_2_);
            }

            return this.templates.containsKey(s) ? (CapsuleTemplate)this.templates.get(s) : null;
        }
    }

    /**
     * This reads a structure template from the given location and stores it.
     * This first attempts get the template from an external folder.
     * If it isn't there then it attempts to take it from the minecraft jar.
     */
    public boolean readTemplate(MinecraftServer server, ResourceLocation id)
    {
        String s = id.getResourcePath();
        File file1 = new File(this.baseFolder, s + ".nbt");

        if (!file1.exists())
        {
            return this.readTemplateFromJar(id);
        }
        else
        {
            InputStream inputstream = null;
            boolean flag;

            try
            {
                inputstream = new FileInputStream(file1);
                this.readTemplateFromStream(s, inputstream);
                return true;
            }
            catch (Throwable var11)
            {
                flag = false;
            }
            finally
            {
                IOUtils.closeQuietly(inputstream);
            }

            return flag;
        }
    }

    /**
     * reads a template from the minecraft jar
     */
    private boolean readTemplateFromJar(ResourceLocation id)
    {
        String s1 = id.getResourcePath();
        InputStream inputstream = null;
        boolean flag;

        try
        {
            LOGGER.info("reading from jar at" + "/" + s1 + ".nbt");
            inputstream = MinecraftServer.class.getResourceAsStream("/" + s1 + ".nbt");
            this.readTemplateFromStream(s1, inputstream);
            return true;
        }
        catch (Throwable var10)
        {
            flag = false;
        }
        finally
        {
            IOUtils.closeQuietly(inputstream);
        }

        return flag;
    }

    /**
     * reads a template from an inputstream
     */
    public void readTemplateFromStream(String id, InputStream stream) throws IOException
    {
        NBTTagCompound nbttagcompound = CompressedStreamTools.readCompressed(stream);
        CapsuleTemplate template = new CapsuleTemplate();
        template.read(nbttagcompound);
        this.templates.put(id, template);
    }

    /**
     * writes the template to an external folder
     */
    public boolean writeTemplate(@Nullable MinecraftServer server, ResourceLocation id)
    {
        String s = id.getResourcePath();

        if (server != null && this.templates.containsKey(s))
        {
            File file1 = new File(this.baseFolder);

            if (!file1.exists())
            {
                if (!file1.mkdirs())
                {
                    return false;
                }
            }
            else if (!file1.isDirectory())
            {
                return false;
            }

            File file2 = new File(file1, s + ".nbt");
            CapsuleTemplate template = (CapsuleTemplate)this.templates.get(s);
            OutputStream outputstream = null;
            boolean flag;

            try
            {
                NBTTagCompound nbttagcompound = template.writeToNBT(new NBTTagCompound());
                outputstream = new FileOutputStream(file2);
                CompressedStreamTools.writeCompressed(nbttagcompound, outputstream);
                return true;
            }
            catch (Throwable var13)
            {
                flag = false;
            }
            finally
            {
                IOUtils.closeQuietly(outputstream);
            }

            return flag;
        }
        else
        {
            return false;
        }
    }

    public void remove(ResourceLocation p_189941_1_)
    {
        this.templates.remove(p_189941_1_.getResourcePath());
    }
}