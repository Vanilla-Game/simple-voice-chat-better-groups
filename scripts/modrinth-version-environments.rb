#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"

module ModrinthVersionEnvironments
  module_function

  def plan(versions, expected_environments)
    expected_environments.map do |version_number, environment|
      matches = versions.select { |version| version.fetch("version_number") == version_number }
      unless matches.length == 1
        raise ArgumentError,
              "expected exactly one Modrinth version #{version_number}, found #{matches.length}"
      end

      {
        "id" => matches.first.fetch("id"),
        "version_number" => version_number,
        "environment" => environment
      }
    end
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} MODRINTH_VERSIONS_JSON VERSION=ENVIRONMENT..." if ARGV.length < 2

  versions = JSON.parse(File.read(ARGV.fetch(0)))
  expected_environments = ARGV.drop(1).to_h do |mapping|
    version_number, environment = mapping.split("=", 2)
    abort "invalid version environment mapping: #{mapping}" if environment.nil? || environment.empty?

    [version_number, environment]
  end

  puts JSON.pretty_generate(ModrinthVersionEnvironments.plan(versions, expected_environments))
end
