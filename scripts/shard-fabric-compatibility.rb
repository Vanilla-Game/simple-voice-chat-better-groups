#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

max_rows = Integer(ARGV.fetch(0, "10"), 10)
abort "max rows must be positive" unless max_rows.positive?

rows = JSON.parse($stdin.read)
abort "expected a JSON array" unless rows.is_a?(Array)

required_keys = %w[target minecraft voicechatArtifact].freeze
rows.each_with_index do |row, index|
  abort "row #{index} must be an object" unless row.is_a?(Hash)

  missing = required_keys.reject { |key| row[key].is_a?(String) && !row[key].empty? }
  abort "row #{index} has missing or invalid fields: #{missing.join(', ')}" unless missing.empty?
end

def slug(value)
  value.downcase.gsub(/[^a-z0-9]+/, "-").sub(/\A-+/, "").sub(/-+\z/, "")
end

groups = rows.group_by { |row| [row.fetch("target"), row.fetch("minecraft")] }
shards = groups.flat_map do |(target, minecraft), group_rows|
  shard_count = (group_rows.length.to_f / max_rows).ceil
  base_size = group_rows.length / shard_count
  larger_shards = group_rows.length % shard_count
  offset = 0

  Array.new(shard_count) do |index|
    size = base_size + (index < larger_shards ? 1 : 0)
    shard_rows = group_rows.slice(offset, size)
    offset += size

    {
      "id" => "target-#{slug(target)}-mc-#{slug(minecraft)}-part-#{index + 1}",
      "target" => target,
      "minecraft" => minecraft,
      "part" => index + 1,
      "parts" => shard_count,
      "rows" => shard_rows
    }
  end
end

shard_ids = shards.map { |shard| shard.fetch("id") }
abort "generated duplicate shard IDs" unless shard_ids.uniq.length == shard_ids.length

puts JSON.generate(shards)
