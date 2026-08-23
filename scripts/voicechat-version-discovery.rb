#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

module VoicechatVersionDiscovery
  module_function

  def core(artifact)
    normalized = artifact.sub(/\A(?:fabric|bukkit)-/, "")
    match = normalized.match(/(\d+\.\d+\.\d+)(?:\+.*)?\z/)
    raise ArgumentError, "cannot extract a three-part version from #{artifact.inspect}" unless match

    match[1].split(".").map(&:to_i)
  end

  def contiguous_after(candidates, maximum)
    unique = {}
    candidates.sort_by { |candidate| [candidate.fetch("core"), candidate.fetch("artifact")] }.each do |candidate|
      unique[candidate.fetch("core")] ||= candidate
    end

    accepted = []
    expected_patch = maximum.fetch(2) + 1
    unique.values.each do |candidate|
      break unless candidate.fetch("core").fetch(2) == expected_patch

      accepted << candidate
      expected_patch += 1
    end
    accepted
  end

  def build(catalog, versions)
    fabric_candidates = catalog.fetch("fabric").fetch("targets").flat_map do |target|
      compatibility = target.fetch("compatibility")
      maximum = compatibility.map { |entry| core(entry.fetch("voicechatArtifact")) }.max
      minor = maximum.first(2)

      candidates = versions.each_with_object([]) do |version, selected|
        next unless listed_release?(version)
        next unless version.fetch("loaders", []).include?("fabric")
        next unless version.fetch("game_versions", []).include?(target.fetch("compile").fetch("minecraft"))
        next unless version.fetch("version_number").start_with?("fabric-")

        candidate_core = core(version.fetch("version_number"))
        next unless candidate_core.first(2) == minor
        next unless (candidate_core <=> maximum) == 1

        selected << {
          "target" => target.fetch("id"),
          "artifact" => version.fetch("version_number"),
          "core" => candidate_core,
          "runtimes" => target.fetch("releaseMinecraftVersions")
        }
      end

      contiguous_after(candidates, maximum)
    end

    server_candidates = catalog.fetch("server").fetch("targets").flat_map do |target|
      maximum = target.fetch("voicechatArtifacts").map { |artifact| core(artifact) }.max
      minor = maximum.first(2)

      candidates = versions.each_with_object([]) do |version, selected|
        next unless listed_release?(version)
        next unless version.fetch("loaders", []).include?("bukkit")
        next unless version.fetch("game_versions", []).include?(target.fetch("minecraft"))
        next unless version.fetch("version_number").start_with?("bukkit-")

        candidate_core = core(version.fetch("version_number"))
        next unless candidate_core.first(2) == minor
        next unless (candidate_core <=> maximum) == 1

        selected << {
          "minecraft" => target.fetch("minecraft"),
          "build" => target.fetch("paperBuild"),
          "artifact" => version.fetch("version_number"),
          "core" => candidate_core
        }
      end

      contiguous_after(candidates, maximum)
    end

    fabric_candidates = unique_sorted(fabric_candidates, %w[target artifact])
    server_candidates = unique_sorted(server_candidates, %w[minecraft artifact])

    {
      "fabric-candidates" => fabric_candidates,
      "server-candidates" => server_candidates,
      "fabric-matrix" => fabric_candidates.flat_map do |candidate|
        candidate.fetch("runtimes").map do |runtime|
          {
            "target" => candidate.fetch("target"),
            "minecraft" => runtime,
            "voicechatArtifact" => candidate.fetch("artifact")
          }
        end
      end,
      "server-matrix" => server_candidates.map do |candidate|
        {
          "platform" => "paper",
          "minecraft" => candidate.fetch("minecraft"),
          "build" => candidate.fetch("build"),
          "voicechatArtifact" => candidate.fetch("artifact")
        }
      end,
      "has-fabric" => !fabric_candidates.empty?,
      "has-server" => !server_candidates.empty?,
      "has-any" => !fabric_candidates.empty? || !server_candidates.empty?
    }
  end

  def listed_release?(version)
    version.fetch("version_type") == "release" && version.fetch("status") == "listed"
  end

  def unique_sorted(candidates, unique_keys)
    candidates.each_with_object({}) do |candidate, unique|
      unique[unique_keys.map { |key| candidate.fetch(key) }] ||= candidate
    end.values.sort_by { |candidate| [candidate.fetch(unique_keys.first), candidate.fetch("core")] }
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} COMPATIBILITY_JSON MODRINTH_VERSIONS_JSON" unless ARGV.length == 2

  catalog = JSON.parse(File.read(ARGV.fetch(0)))
  versions = JSON.parse(File.read(ARGV.fetch(1)))
  puts JSON.generate(VoicechatVersionDiscovery.build(catalog, versions))
end
