package NokhXyr.NokhFrame;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Optional Iris/Oculus integration through the public Iris API ({@code net.irisshaders.iris.api.v0.IrisApi}),
 * looked up by reflection so Nokh Frame has no dependency on either mod.
 */
public final class ShaderSupport {
    private static boolean resolved;
    private static @Nullable Object api;
    private static @Nullable Method inUse;

    private ShaderSupport() {
    }

    /** True while a shaderpack renders the world; GUI rendering is never processed by shaderpacks. */
    public static boolean shaderPackInUse() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError exception) {
                api = null;
                inUse = null;
            }
        }
        if (api == null || inUse == null) return false;
        try {
            return (Boolean) inUse.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }
}
