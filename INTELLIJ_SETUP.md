# IntelliJ IDEA Setup Guide

## Fixing "Module not specified" and Orange File Tree Issues

### Step 1: Reimport Gradle Project

1. **Close IntelliJ IDEA** (if it's open)

2. **Delete IntelliJ cache files** (optional but recommended):
   - Delete the `.idea` folder in your project root
   - Or just delete `.idea/modules.xml` and `.idea/misc.xml`

3. **Reopen IntelliJ IDEA** and open your project

4. **Import as Gradle Project**:
   - If IntelliJ doesn't automatically detect it as a Gradle project:
     - Go to `File` → `Settings` (or `IntelliJ IDEA` → `Preferences` on Mac)
     - Navigate to `Build, Execution, Deployment` → `Build Tools` → `Gradle`
     - Make sure "Use Gradle from" is set to `'wrapper' task in Gradle build script`
     - Check "Use auto-import"

5. **Sync Gradle**:
   - Look for a notification banner at the top saying "Gradle project needs to be imported"
   - Click "Import Gradle Project"
   - Or manually: Right-click on `build.gradle` → `Import Gradle Project`
   - Or use the Gradle tool window: Click the refresh icon (🔄)

### Step 2: Fix Module Configuration

1. **Open Run Configuration**:
   - Go to `Run` → `Edit Configurations...`
   - Select your `Lwjgl3Launcher` configuration

2. **Set the Module**:
   - In the "Build and run" section, find the "Use classpath of module" dropdown
   - Select `ApopulisMap-lwjgl3` (or `lwjgl3` module)
   - If you don't see it, make sure Gradle sync completed successfully

3. **Verify Main Class**:
   - Main class should be: `si.apopulis.map.lwjgl3.Lwjgl3Launcher`
   - Working directory should be: `$PROJECT_DIR$/assets` or the `assets` folder path

### Step 3: Fix Orange File Tree (Excluded Files)

Orange files/folders mean they're excluded from indexing. To fix:

1. **Check Project Structure**:
   - Go to `File` → `Project Structure` (or `Ctrl+Alt+Shift+S` / `Cmd+;` on Mac)
   - Click on `Modules` in the left sidebar
   - Select each module (`core`, `lwjgl3`, `android`)
   - Click on the `Sources` tab
   - Make sure your source folders are marked as "Sources" (blue) not "Excluded" (red)

2. **Check Excluded Directories**:
   - In `Project Structure` → `Modules` → Select a module
   - Click on the `Excluded` tab
   - Remove any directories that shouldn't be excluded (like `src/main/java`)

3. **Invalidate Caches**:
   - Go to `File` → `Invalidate Caches...`
   - Check "Clear file system cache and Local History"
   - Click "Invalidate and Restart"
   - After restart, let IntelliJ reindex the project

### Step 4: Verify Everything Works

1. **Check Gradle Tool Window**:
   - Open the Gradle tool window (View → Tool Windows → Gradle)
   - You should see your project structure with `core`, `lwjgl3`, `android` modules
   - Expand `ApopulisMap-lwjgl3` → `Tasks` → `application` → `run`
   - Right-click `run` and select "Run" to test

2. **Check Project View**:
   - Files should no longer be orange
   - You should see proper package structure in `core/src/main/java/si/apopulis/map/`

3. **Run Configuration**:
   - The "Module not specified" warning should be gone
   - You should be able to run the application

### Alternative: Manual Module Setup (if Gradle import fails)

If Gradle import doesn't work:

1. Go to `File` → `Project Structure` → `Modules`
2. Click `+` → `Import Module`
3. Select the `core` folder → Choose "Import module from external model" → "Gradle"
4. Repeat for `lwjgl3` module
5. Make sure `lwjgl3` depends on `core`:
   - Select `lwjgl3` module → `Dependencies` tab
   - Click `+` → `Module Dependency` → Select `core`

### Troubleshooting

- **If files are still orange**: Check `File` → `Settings` → `Project` → `Project Structure` → `Modules` → `Excluded` tab
- **If module still not found**: Make sure Gradle sync completed. Check the Gradle tool window for errors
- **If build fails**: Check that Java SDK is configured: `File` → `Project Structure` → `Project` → `SDK`

