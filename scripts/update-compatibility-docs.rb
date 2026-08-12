#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

ROOT = File.expand_path("..", __dir__)
REPOSITORY = "Vanilla-Game/simple-voice-chat-better-groups"

def artifact_core(artifact)
  artifact.sub(/^(fabric|bukkit)-/, "").sub(/\+.*/, "").split("-").last
end

def artifact_range(artifacts)
  versions = artifacts.map { |artifact| artifact_core(artifact) }.uniq.sort_by do |version|
    version.split(".").map(&:to_i)
  end
  raise "Artifact list must not be empty" if versions.empty?

  "#{versions.first}–#{versions.last}"
end

def declared_range(range)
  match = range.match(/\A>=(?:\d+\.\d+\.\d+-)?(\d+\.\d+\.\d+) <(?:\d+\.\d+\.\d+-)?(\d+)\.(\d+)\.(\d+)\z/) or
    raise "Invalid range: #{range}"
  "#{match[1]}–#{match[2]}.#{match[3]}.#{match[4].to_i - 1}"
end

def download_link(filename, version)
  url = "https://github.com/#{REPOSITORY}/releases/download/v#{version}/#{filename}"
  "[`#{filename}`](#{url})"
end

def minecraft_versions(target)
  versions = target.fetch("releaseMinecraftVersions")
  raise "releaseMinecraftVersions must not be empty for #{target.fetch('id')}" if versions.empty?

  return "#{versions.first}–#{versions.last}" if versions.length > 1

  dependency = target.fetch("minecraftDependency")
  dependency.end_with?(".x") ? dependency : versions.first
end

def markdown_table(headers, rows)
  widths = headers.each_index.map do |index|
    ([headers[index]] + rows.map { |row| row[index] }).map(&:length).max
  end
  render = lambda do |values|
    "| #{values.each_with_index.map { |value, index| value.ljust(widths[index]) }.join(' | ')} |"
  end

  ([render.call(headers), render.call(widths.map { |width| "-" * width })] + rows.map(&render)).join("\n")
end

def replace_generated_block(content, name, table)
  start_marker = "<!-- generated:#{name}:start -->"
  end_marker = "<!-- generated:#{name}:end -->"
  unless content.scan(start_marker).length == 1 && content.scan(end_marker).length == 1
    raise "README.md must contain exactly one #{start_marker} and one #{end_marker}"
  end

  pattern = /#{Regexp.escape(start_marker)}.*?#{Regexp.escape(end_marker)}/m
  content.sub(pattern, "#{start_marker}\n\n#{table}\n\n#{end_marker}")
end

abort "Usage: #{File.basename($PROGRAM_NAME)} [--check]" unless ARGV.empty? || ARGV == ["--check"]

catalog = JSON.parse(File.read(File.join(ROOT, "compatibility.json")))
manifest = JSON.parse(File.read(File.join(ROOT, ".release-please-manifest.json")))
version = manifest.fetch(".")
raise "Invalid release version: #{version}" unless version.match?(/\A\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?\z/)

build = File.read(File.join(ROOT, "build.gradle.kts"))
build_version = build[/^version = "([^"]+)"/, 1] or raise "Project version not found in build.gradle.kts"
raise "Release manifest version #{version} does not match build version #{build_version}" unless version == build_version

server_filename = "svc-better-groups-#{version}.jar"
server_rows = catalog.fetch("server").fetch("targets").map do |target|
  software = ["Paper"]
  software << "Leaf (experimental)" if target.key?("leafBuild")
  [
    download_link(server_filename, version),
    "`#{target.fetch('minecraft')}`",
    software.join("; "),
    "`#{catalog.fetch('java')}`+",
    "Bukkit `#{artifact_range(target.fetch('voicechatArtifacts')).sub('–', '`–`')}`"
  ]
end
server_table = markdown_table(
  ["Artifact", "Minecraft", "Server software", "Java", "Simple Voice Chat"],
  server_rows
)

fabric_rows = catalog.fetch("fabric").fetch("targets").map do |target|
  compile = target.fetch("compile")
  filename = "#{target.fetch('archiveBaseName')}-#{version}.jar"
  [
    download_link(filename, version),
    "`#{minecraft_versions(target).sub('–', '`–`')}`",
    "Fabric Loader `#{compile.fetch('fabricLoader')}`+",
    "`#{compile.fetch('fabricApi')}`+",
    "`#{target.fetch('java')}`+",
    "Fabric `#{declared_range(target.fetch('voicechatRange')).sub('–', '`–`')}`"
  ]
end
fabric_table = markdown_table(
  ["Artifact", "Minecraft", "Mod loader", "Fabric API", "Java", "Simple Voice Chat"],
  fabric_rows
)

readme_path = File.join(ROOT, "README.md")
original = File.read(readme_path)
updated = replace_generated_block(original, "server-downloads", server_table)
updated = replace_generated_block(updated, "fabric-downloads", fabric_table)
changed = updated != original

if ARGV == ["--check"]
  abort "Compatibility documentation is stale: README.md" if changed
elsif changed
  File.write(readme_path, updated)
end
