## 1. If your mod have some incompatibilities you want to fix

Tips :
- Anything working with vanilla structure block will work with capsule. The easiest way to test is to setup 2 structures blocks, one saving to a template, the other loading the template. Adjust the code until the loaded area is working as expected in regard of the saved one.

- Due to the way capsule works, no tileEntity can be duplicated. It means that a tileEntity carrying a unique id is safe as it won't be duplicated:  it will always be removed before replaced (eventually elsewhere).

- Note that the use of absolute coordinate to find locations inside the capsule will break (content will be moved). When it make sense: relative coordinates should always be prefered over absolute coordinates.

## 2. If Your mod have incompatibilities with capsule and you want capsule to ignore your block(s)

You sure it can't be fixed :'(    
If so, it should't take long, follow the steps according to the version you are targeting:

### 1.15 or newer
Add the blocks to exclude to the `capsule:excluded` tag. 

Documentation: https://mcforge.readthedocs.io/en/latest/utilities/tags/.     
TLDR: there should be a `data/capsule/tags/blocks/excluded.json` file in your mod resources folder that contains something like the exemple below. Be sure that replace is "false" to keep configuration working for other mods.
```
{
    "replace": false,
    "values": [
        "tombstone:player_graves"
    ]
}
```

### 1.12
Open an issue on github and provide the block id to be excluded by default. I'll update the mod with the new configuration.

### older
Not updated anymore. You can still add the block id in the `capsule.cfg` file under "opExcludedBlocks" when distributing a modpack.