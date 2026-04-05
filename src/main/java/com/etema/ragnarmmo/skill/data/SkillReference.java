package com.etema.ragnarmmo.skill.data;

/**
 * Optional external reference metadata for a skill definition.
 */
public record SkillReference(
        String sourceName,
        String sourceUrl,
        String notes) {
}
