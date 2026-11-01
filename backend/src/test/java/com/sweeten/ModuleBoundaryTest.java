package com.sweeten;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块边界（可执行的架构约定，说明见 docs/ARCHITECTURE.md）。
 * <p>
 * com.sweeten 下每个一级包是一个业务模块。下表列出每个模块<b>允许</b>依赖的其它模块，
 * 依赖只能自上而下，不允许出现环：
 * <pre>
 *   storefront ─▶ customer ─▶ order ─▶ catalog
 *        │            └──▶ promotion
 *        └─▶ catalog / order / promotion
 *   analytics（只读统计，经自己的 AnalyticsMapper 读表）
 *   staff / catalog / inventory / promotion / media ─▶ audit ─▶ identity ─▶ common
 * </pre>
 * 新增模块或新的依赖方向时，先在 docs/ARCHITECTURE.md 说明理由，再更新本表。
 */
@DisplayName("模块边界")
class ModuleBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/sweeten");

    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            entry("common", Set.of()),
            entry("identity", Set.of("common")),
            entry("audit", Set.of("common", "identity")),
            entry("media", Set.of("common", "identity", "audit")),
            entry("staff", Set.of("common", "identity", "audit")),
            entry("catalog", Set.of("common", "identity", "audit")),
            entry("inventory", Set.of("common", "identity", "audit")),
            entry("promotion", Set.of("common", "identity", "audit")),
            entry("order", Set.of("common", "identity", "audit", "catalog")),
            entry("customer", Set.of("common", "identity", "audit", "order", "promotion")),
            entry("storefront", Set.of("common", "catalog", "order", "customer", "promotion")),
            entry("analytics", Set.of("common", "identity"))
    );

    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?com\\.sweeten\\.([a-z]+)\\.", Pattern.MULTILINE);

    @Test
    @DisplayName("每个模块只依赖允许的模块")
    void modulesOnlyDependOnAllowedModules() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Path relative = SOURCE_ROOT.relativize(file);
                if (relative.getNameCount() < 2) {
                    continue; // 根包下的启动类可以引用任何模块
                }
                String module = relative.getName(0).toString();
                Set<String> allowed = ALLOWED.getOrDefault(module, Set.of());
                Matcher m = IMPORT.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    String target = m.group(1);
                    if (!target.equals(module) && !allowed.contains(target)) {
                        violations.add(module + " -> " + target + "  (" + relative.toString().replace('\\', '/') + ")");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "违反模块边界的依赖：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("所有模块都登记在依赖表中")
    void everyModuleIsRegistered() throws IOException {
        try (Stream<Path> dirs = Files.list(SOURCE_ROOT)) {
            Set<String> modules = dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
            assertEquals(new TreeSet<>(ALLOWED.keySet()), modules,
                    "新增 / 删除模块时请同步更新 ModuleBoundaryTest.ALLOWED 与 docs/ARCHITECTURE.md");
        }
    }
}
