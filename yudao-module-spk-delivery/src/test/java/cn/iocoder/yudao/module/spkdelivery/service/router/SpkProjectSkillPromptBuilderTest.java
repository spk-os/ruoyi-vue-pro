package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkProjectSkillPromptBuilderTest {

    @TempDir
    Path projectRoot;

    @Test
    void missingProjectSkillIsOptional() {
        assertEquals("base", SpkProjectSkillPromptBuilder.append("base", projectRoot.toString(), "concept"));
    }

    @Test
    void freezesProjectSkillContentAndHash() throws Exception {
        Path skill = projectRoot.resolve(".ipd/skill/concept/SKILL.md");
        Files.createDirectories(skill.getParent());
        Files.writeString(skill, "---\nname: concept-quick\ndescription: quick concept\n---\n只做快速概念分析。\n");

        String prompt = SpkProjectSkillPromptBuilder.append("base", projectRoot.toString(), "concept");
        assertTrue(prompt.contains("Cortex 冻结的项目专用 Skill"));
        assertTrue(prompt.contains("concept-quick"));
        assertTrue(prompt.contains("sha256:"));
        assertTrue(prompt.contains("不得削弱 commercial-release"));
        assertTrue(prompt.contains(skill.toString()));
    }

    @Test
    void malformedExistingSkillFailsClosed() throws Exception {
        Path skill = projectRoot.resolve(".ipd/skill/verify/SKILL.md");
        Files.createDirectories(skill.getParent());
        Files.writeString(skill, "missing frontmatter");
        assertThrows(IllegalStateException.class,
                () -> SpkProjectSkillPromptBuilder.append("base", projectRoot.toString(), "qualify"));
    }
}
