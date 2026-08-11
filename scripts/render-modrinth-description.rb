#!/usr/bin/env ruby
# frozen_string_literal: true

START_MARKER = "<!-- modrinth:start -->"
END_MARKER = "<!-- modrinth:end -->"
EXCLUDE_START_MARKER = "<!-- modrinth:exclude:start -->"
EXCLUDE_END_MARKER = "<!-- modrinth:exclude:end -->"

root = File.expand_path("..", __dir__)
readme = File.read(File.join(root, "README.md"))

unless readme.scan(START_MARKER).length == 1 && readme.scan(END_MARKER).length == 1
  abort "README.md must contain exactly one #{START_MARKER} and one #{END_MARKER}"
end

before, marked = readme.split(START_MARKER, 2)
description, after = marked.split(END_MARKER, 2)
abort "README.md has invalid Modrinth marker order" unless before && description && after

exclude_starts = description.scan(EXCLUDE_START_MARKER).length
exclude_ends = description.scan(EXCLUDE_END_MARKER).length
unless exclude_starts == exclude_ends
  abort "README.md must contain matching #{EXCLUDE_START_MARKER} and #{EXCLUDE_END_MARKER} markers"
end

description = description.gsub(
  /#{Regexp.escape(EXCLUDE_START_MARKER)}.*?#{Regexp.escape(EXCLUDE_END_MARKER)}/m,
  ""
)
if description.include?(EXCLUDE_START_MARKER) || description.include?(EXCLUDE_END_MARKER)
  abort "README.md has invalid Modrinth exclusion marker order"
end

description = description.gsub(/\n{3,}/, "\n\n")
description = description.strip
abort "README.md Modrinth section is empty" if description.empty?

destinations = description.scan(/!?\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)/).flatten
destinations.concat(description.scan(/^\s*\[[^\]]+\]:\s*<?([^\s>]+)>?/).flatten)
destinations.concat(description.scan(/\b(?:href|src)=["']([^"']+)["']/i).flatten)

relative = destinations.reject do |destination|
  destination.match?(%r{\Ahttps://}i) || destination.start_with?("#")
end
abort "Modrinth description contains relative or insecure links: #{relative.uniq.join(', ')}" unless relative.empty?

puts description
