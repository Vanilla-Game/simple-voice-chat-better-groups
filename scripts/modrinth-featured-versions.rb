#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

module ModrinthFeaturedVersions
  module_function

  def unfeature_ids(versions, expected_numbers)
    present_numbers = versions.map { |version| version.fetch("version_number") }
    missing = expected_numbers.reject { |number| present_numbers.include?(number) }
    raise ArgumentError, "Modrinth is missing expected versions: #{missing.join(', ')}" unless missing.empty?

    current = versions.select { |version| expected_numbers.include?(version.fetch("version_number")) }

    versions.select { |version| version["featured"] == true }.each_with_object([]) do |previous, ids|
      ids << previous.fetch("id") if current.any? { |candidate| supersedes?(candidate, previous) }
    end.uniq.sort
  end

  def supersedes?(candidate, previous)
    return false if candidate.fetch("id") == previous.fetch("id")

    candidate_date = candidate["date_published"] || ""
    previous_date = previous["date_published"] || ""
    return false if candidate_date.empty? || previous_date.empty?
    return false unless previous_date < candidate_date
    return false unless subset?(previous["game_versions"] || [], candidate["game_versions"] || [])
    return false unless subset?(previous["loaders"] || [], candidate["loaders"] || [])

    (previous["version_type"] || "release") == (candidate["version_type"] || "release")
  end

  def subset?(values, candidate_values)
    values.all? { |value| candidate_values.include?(value) }
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} MODRINTH_VERSIONS_JSON EXPECTED_VERSION..." if ARGV.length < 2

  versions = JSON.parse(File.read(ARGV.fetch(0)))
  puts ModrinthFeaturedVersions.unfeature_ids(versions, ARGV.drop(1))
end
