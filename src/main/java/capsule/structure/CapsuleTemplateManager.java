package capsule.structure;

import com.google.common.collect.Maps;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
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
    private final DataFixer fixer;

    public CapsuleTemplateManager(String p_i47239_1_, DataFixer p_i47239_2_)
    {
        this.baseFolder = p_i47239_1_;
        this.fixer = p_i47239_2_;
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
    public CapsuleTemplate get(@Nullable MinecraftServer server, ResourceLocation templatePath)
    {
        String s = templatePath.getResourcePath();

        if (this.templates.containsKey(s))
        {
            return (CapsuleTemplate)this.templates.get(s);
        }
        else
        {
            if (server == null)
            {
                this.readTemplateFromJar(templatePath);
            }
            else
            {
                this.readTemplate(templatePath);
            }

            return this.templates.containsKey(s) ? (CapsuleTemplate)this.templates.get(s) : null;
        }
    }

    /**
     * This reads a structure template from the given location and stores it.
     * This first attempts get the template from an external folder.
     * If it isn't there then it attempts to take it from the minecraft jar.
     */
    public boolean readTemplate(ResourceLocation server)
    {
        String s = server.getResourcePath();
        File file1 = new File(this.baseFolder, s + ".nbt");

        if (!file1.exists())
        {
            // first try to read from schematic. If not working try from jar
            return this.readTemplateFromSchematic(server) || this.readTemplateFromJar(server);
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
    }

    /**
     * reads a template from the minecraft jar
     */
    private boolean readTemplateFromJar(ResourceLocation id)
    {
        String s = id.getResourceDomain();
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

        if (!nbttagcompound.hasKey("DataVersion", 99))
        {
            nbttagcompound.setInteger("DataVersion", 500);
        }

        CapsuleTemplate template = new CapsuleTemplate();
        template.read(this.fixer.process(FixTypes.STRUCTURE, nbttagcompound));
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

    public void remove(ResourceLocation templatePath)
    {
        this.templates.remove(templatePath.getResourcePath());
    }

    public boolean readTemplateFromSchematic(ResourceLocation server)
    {
        String s = server.getResourcePath();
        File file1 = new File(this.baseFolder, s + ".schematic");

        if (!file1.exists())
        {
            return false;
        }
        else
        {
            InputStream inputstream = null;
            boolean flag;

            try
            {
                inputstream = new FileInputStream(file1);
                NBTTagCompound schematicNBT = CompressedStreamTools.readCompressed(inputstream);
                CapsuleTemplate template = new CapsuleTemplate();
                template.readSchematic(schematicNBT);
                this.templates.put(s, template);
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
    }
}