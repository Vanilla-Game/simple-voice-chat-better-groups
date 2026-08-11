#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

root = File.expand_path("..", __dir__)
catalog = JSON.parse(File.read(File.join(root, "compatibility.json")))

def artifact_core(artifact)
  artifact.sub(/^(fabric|bukkit)-/, "").sub(/\+.*/, "")
end

def artifact_range(artifacts)
  versions = artifacts.map { |artifact| artifact_core(artifact) }.uniq.sort_by { |version| version.split(".").map(&:to_i) }
  "#{versions.first}–#{versions.last}"
end

def declared_range(range)
  match = range.match(/\A>=(\d+\.\d+\.\d+) <(\d+)\.(\d+)\.(\d+)\z/) or raise "Invalid range: #{range}"
  "#{match[1]}–#{match[2]}.#{match[3]}.#{match[4].to_i - 1}"
end

server = catalog.fetch("server").fetch("targets").to_h { |target| [target.fetch("minecraft"), target] }
fabric = catalog.fetch("fabric").fetch("targets").to_h { |target| [target.fetch("id"), target] }

server_26_1 = artifact_range(server.fetch("26.1.2").fetch("voicechatArtifacts"))
server_26_2 = artifact_range(server.fetch("26.2").fetch("voicechatArtifacts"))
fabric_26_1 = declared_range(fabric.fetch("26.1").fetch("voicechatRange"))
fabric_26_2 = declared_range(fabric.fetch("26.2").fetch("voicechatRange"))
release_assets_url = "https://github.com/Vanilla-Game/simple-voice-chat-better-groups/releases/latest#assets"

replacements = {
  "README.md" => [
    [
      /\| \[`svc-better-groups-<version>\.jar`\]\([^\n]+\) \| `26\.1\.2` \| Paper; Leaf \(experimental\) \| `25`\+ \| Bukkit `\d+\.\d+\.\d+`–`\d+\.\d+\.\d+` \|/,
      "| [`svc-better-groups-<version>.jar`](#{release_assets_url}) | `26.1.2` | Paper; Leaf (experimental) | `25`+ | Bukkit `#{server_26_1.split('–').join('`–`')}` |"
    ],
    [
      /\| \[`svc-better-groups-<version>\.jar`\]\([^\n]+\) \| `26\.2` \| Paper; Leaf \(experimental\) \| `25`\+ \| Bukkit `\d+\.\d+\.\d+`–`\d+\.\d+\.\d+` \|/,
      "| [`svc-better-groups-<version>.jar`](#{release_assets_url}) | `26.2` | Paper; Leaf (experimental) | `25`+ | Bukkit `#{server_26_2.split('–').join('`–`')}` |"
    ],
    [
      /\| \[`svc-better-groups-fabric-26\.1-<version>\.jar`\]\([^\n]+\) \| `26\.1`–`26\.1\.2` \| Fabric Loader `0\.18\.4`\+ \| `0\.144\.3\+26\.1`\+ \| `25`\+ \| Fabric `\d+\.\d+\.\d+`–`\d+\.\d+\.\d+` \|/,
      "| [`svc-better-groups-fabric-26.1-<version>.jar`](#{release_assets_url}) | `26.1`–`26.1.2` | Fabric Loader `0.18.4`+ | `0.144.3+26.1`+ | `25`+ | Fabric `#{fabric_26_1.split('–').join('`–`')}` |"
    ],
    [
      /\| \[`svc-better-groups-fabric-26\.2-<version>\.jar`\]\([^\n]+\) \| `26\.2\.x` \| Fabric Loader `0\.19\.3`\+ \| `0\.152\.1\+26\.2`\+ \| `25`\+ \| Fabric `\d+\.\d+\.\d+`–`\d+\.\d+\.\d+` \|/,
      "| [`svc-better-groups-fabric-26.2-<version>.jar`](#{release_assets_url}) | `26.2.x` | Fabric Loader `0.19.3`+ | `0.152.1+26.2`+ | `25`+ | Fabric `#{fabric_26_2.split('–').join('`–`')}` |"
    ]
  ],
  "assets/modrinth-description.md" => [
    [/- server 26\.1\.2: Bukkit SVC \d+\.\d+\.\d+–\d+\.\d+\.\d+;/, "- server 26.1.2: Bukkit SVC #{server_26_1};"],
    [/- server 26\.2: Bukkit SVC \d+\.\d+\.\d+–\d+\.\d+\.\d+;/, "- server 26.2: Bukkit SVC #{server_26_2};"],
    [/- Fabric client 26\.1\.x: SVC \d+\.\d+\.\d+–\d+\.\d+\.\d+;/, "- Fabric client 26.1.x: SVC #{fabric_26_1};"],
    [/- Fabric client 26\.2\.x: SVC \d+\.\d+\.\d+–\d+\.\d+\.\d+\./, "- Fabric client 26.2.x: SVC #{fabric_26_2}."],
  ]
}

check_only = ARGV == ["--check"]
changed = []

replacements.each do |relative_path, rules|
  path = File.join(root, relative_path)
  original = File.read(path)
  updated = rules.reduce(original) do |content, (pattern, replacement)|
    raise "Expected compatibility text not found in #{relative_path}: #{pattern.inspect}" unless content.match?(pattern)

    content.sub(pattern, replacement)
  end
  next if updated == original

  changed << relative_path
  File.write(path, updated) unless check_only
end

abort "Compatibility documentation is stale: #{changed.join(', ')}" if check_only && !changed.empty?
