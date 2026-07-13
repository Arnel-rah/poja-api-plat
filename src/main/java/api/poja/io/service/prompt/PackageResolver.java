package api.poja.io.service.prompt;

public final class PackageResolver {

    private PackageResolver() {
    }

    public static String toPackageName(String appName) {
        String normalized = appName.toLowerCase().replaceAll("[^a-z0-9]", "");
        return "com." + normalized + ".api";
    }

    public static String toBasePath(String appName) {
        String packageName = toPackageName(appName);
        return "generated/" + appName + "/src/main/java/" + packageName.replace(".", "/") + "/";
    }

    public static String toResourcesPath(String appName) {
        return "generated/" + appName + "/src/main/resources/";
    }

    public static String toMainClassName(String appName) {
        String[] parts = appName.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.append("Application").toString();
    }
}