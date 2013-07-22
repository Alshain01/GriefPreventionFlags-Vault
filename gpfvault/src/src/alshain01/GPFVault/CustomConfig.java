package src.alshain01.GPFVault;
import java.io.File;
import java.io.InputStream;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Modified YAML manager from http://wiki.bukkit.org/Configuration_API_Reference
 * 
 * @author bukkit.org
 */
public class CustomConfig {
	private static JavaPlugin plugin;
	private String dataFile;
    private File customConfigFile = null;
    private FileConfiguration customConfig = null;
    
    /**
     * Class Constructor
     * @param dataFile The file name for this CustomConfig
     */
    protected CustomConfig(JavaPlugin plugin, String dataFile) {
		CustomConfig.plugin = plugin;
    	this.dataFile = dataFile;
    }
    
    /**
     * Reloads the YAML from file to the MemorySection
     */
    protected void reloadCustomConfig() {
        if (customConfigFile == null) {
        	customConfigFile = new File(plugin.getDataFolder(), this.dataFile);
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
     
        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(this.dataFile);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customConfig.setDefaults(defConfig);
        }
    }
    
    /**
     * @return The file configuration for this CustomConfig
     */
    protected FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            this.reloadCustomConfig();
        }
        return customConfig;
    }
    
    /**
     * Save the default file for this CustomConfig to working plugin directory file.
     */
    protected void saveDefaultConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(plugin.getDataFolder(), this.dataFile);
        }
        if (!customConfigFile.exists()) {            
        	GPFVault.instance.saveResource(this.dataFile, false);
         }
    }
}
