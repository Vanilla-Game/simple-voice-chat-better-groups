#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

module VoicechatCompatibilityUpdate
  module_function

  def apply(catalog, fabric_candidates, server_candidates, results_directory)
    changes = []
    messages = []

    grouped(fabric_candidates, "target").each do |target_name, candidates|
      green, failed = green_prefix(candidates) do |candidate|
        candidate.fetch("runtimes").all? do |runtime|
          result_ok?(results_directory, ["fabric", target_name, runtime, candidate.fetch("artifact")])
        end
      end
      messages << "Stopping Fabric #{target_name} at #{failed.fetch('artifact')}: at least one runtime failed." if failed
      next if green.empty?

      target = catalog.fetch("fabric").fetch("targets").find { |entry| entry.fetch("id") == target_name }
      raise KeyError, "unknown Fabric target #{target_name.inspect}" unless target

      green.each do |candidate|
        candidate.fetch("runtimes").each do |runtime|
          target.fetch("compatibility") << {
            "minecraft" => runtime,
            "voicechatArtifact" => candidate.fetch("artifact")
          }
        end
      end

      old_range = target.fetch("voicechatRange")
      lower, upper = old_range.split(" <", 2)
      raise ArgumentError, "invalid voicechatRange #{old_range.inspect}" unless lower && upper

      upper_prefix = upper.include?("-") ? upper.sub(/[^-]*\z/, "") : ""
      version = green.last.fetch("core")
      new_range = "#{lower} <#{upper_prefix}#{version.fetch(0)}.#{version.fetch(1)}.#{version.fetch(2) + 1}"
      target["voicechatRange"] = new_range
      changes << "Fabric #{target_name} -> #{new_range}"
    end

    grouped(server_candidates, "minecraft").each do |minecraft, candidates|
      green, failed = green_prefix(candidates) do |candidate|
        result_ok?(results_directory, ["server", "paper", minecraft, candidate.fetch("artifact")])
      end
      messages << "Stopping Bukkit #{minecraft} at #{failed.fetch('artifact')}: compatibility check failed." if failed
      next if green.empty?

      target = catalog.fetch("server").fetch("targets").find do |entry|
        entry.fetch("minecraft") == minecraft
      end
      raise KeyError, "unknown server target #{minecraft.inspect}" unless target

      green.each do |candidate|
        target.fetch("voicechatArtifacts") << candidate.fetch("artifact")
        changes << "Bukkit #{minecraft} -> #{candidate.fetch('artifact')}"
      end
    end

    {
      "updated" => !changes.empty?,
      "summary" => changes.join(","),
      "messages" => messages
    }
  end

  def grouped(candidates, key)
    candidates.group_by { |candidate| candidate.fetch(key) }
              .sort_by { |value, _| value }
              .to_h
  end

  def green_prefix(candidates)
    green = []
    failed = nil
    candidates.sort_by { |candidate| [candidate.fetch("core"), candidate.fetch("artifact")] }.each do |candidate|
      unless yield(candidate)
        failed = candidate
        break
      end

      green << candidate
    end
    [green, failed]
  end

  def result_ok?(directory, components)
    path = File.join(directory, components.join("__"))
    File.file?(path) && File.read(path).sub(/\n+\z/, "") == "ok"
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} COMPATIBILITY_JSON FABRIC_CANDIDATES_JSON SERVER_CANDIDATES_JSON RESULTS_DIRECTORY" unless ARGV.length == 4

  catalog_path = ARGV.fetch(0)
  catalog = JSON.parse(File.read(catalog_path))
  fabric_candidates = JSON.parse(File.read(ARGV.fetch(1)))
  server_candidates = JSON.parse(File.read(ARGV.fetch(2)))
  result = VoicechatCompatibilityUpdate.apply(catalog, fabric_candidates, server_candidates, ARGV.fetch(3))

  if result.fetch("updated")
    temporary_path = "#{catalog_path}.new"
    File.write(temporary_path, "#{JSON.pretty_generate(catalog)}\n")
    File.rename(temporary_path, catalog_path)
  end

  puts JSON.generate(result)
end
