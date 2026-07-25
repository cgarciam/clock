# Clock

## Running from the console

```
mvn compile exec:exec -Dclock.width=68 -Dclock.height=20 -Dclock.font.size=10 -Djavafx.lib=/path/to/javafx-sdk-17
```

## Running from STS / Eclipse

**Important:** Run `home.clock.Launcher` as the main class, **not** `MainModal`.

Since JDK 11+, JavaFX is no longer bundled with the JDK. If you run `MainModal` directly you will get:

> Error: JavaFX runtime components are missing, and are required to run this application

The `Launcher` class is a plain (non-JavaFX) entry point that delegates to `MainModal`, bypassing this restriction.

### Steps

1. **Run → Run Configurations → Java Application**
2. Set **Main class** to `home.clock.Launcher`
3. (Optional) In the **Arguments** tab, add these **VM arguments**:
   ```
   --module-path "C:\path\to\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml
   ```
4. (Optional) In **Program arguments**, add size parameters:
   ```
   --width=68 --height=20 --fontSize=10
   ```