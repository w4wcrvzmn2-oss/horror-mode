package ru.exeswi.exest.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu hook. This class is only ever loaded by ModMenu itself, so the mod runs
 * fine when ModMenu is not installed — the config then lives in the JSON file.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HorrorConfigScreen::new;
    }
}
