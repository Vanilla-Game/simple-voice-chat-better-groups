#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "rubygems/version"
require_relative "voicechat-version-discovery"

module CiCompatibilityPlan
  MODES = %w[none smoke release full].freeze
  SHARED_PATHS = %r{\A(?:compatibility\.json\z|(?:build|settings)\.gradle\.kts\z|gradle\.properties\z|gradle/|gradlew(?:\.bat)?\z|\.github/workflows/(?:build|release-please|voicechat-compat|voicechat-discovery|server-compat)\.yml\z|scripts/(?:ci-compatibility-plan|apply-voicechat-compatibility-results|merge-compatibility-results|voicechat-version-discovery)\.rb\z|test/scripts/(?:ci_compatibility_plan|merge_compatibility_results)_test\.rb\z)}

  module_function

  def build(catalog, event_name:, head_ref: "", input_mode: "", changed_files: [])
    mode, client, server = selection(event_name, head_ref, input_mode, changed_files)
    selected = matrices(catalog, mode: mode, run_client: client, run_server: server)
    {
      "mode" => mode,
      "run-client" => !selected.fetch("fabric").empty?,
      "run-server" => !selected.fetch("server").empty?
    }.merge(selected)
  end

  def selection(event_name, head_ref, input_mode, changed_files)
    case event_name
    when "schedule"
      ["full", true, true]
    when "workflow_dispatch"
      validate_mode!(input_mode)
      [input_mode, true, true]
    when "pull_request"
      return ["release", true, true] if head_ref.start_with?("release-please--branches--main--components--")

      shared = changed_files.any? { |path| SHARED_PATHS.match?(path) }
      client = shared || changed_files.any? { |path| path.start_with?("client-fabric/") || %w[scripts/run-client-compat.py test/scripts/test_run_client_compat.py].include?(path) }
      server = shared || changed_files.any? { |path| path.start_with?("src/") }
      [client || server ? "smoke" : "none", client, server]
    when "push"
      ["none", false, false]
    else
      raise ArgumentError, "unsupported event: #{event_name.inspect}"
    end
  end

  def matrices(catalog, mode:, run_client: true, run_server: true)
    validate_mode!(mode)
    return { "fabric" => [], "server" => [] } if mode == "none"

    fabric = run_client ? fabric_matrix(catalog, mode) : []
    server = run_server ? server_matrix(catalog, mode) : []
    { "fabric" => fabric, "server" => server }
  end

  def fabric_matrix(catalog, mode)
    catalog.fetch("fabric").fetch("targets").sort_by { |target| Gem::Version.new(target.fetch("id")) }.flat_map do |target|
      rows = target.fetch("compatibility").uniq.sort_by do |row|
        [Gem::Version.new(row.fetch("minecraft")), artifact_order(row.fetch("voicechatArtifact"))]
      end
      selected = case mode
                 when "smoke"
                   rows.empty? ? [] : [rows.last]
                 when "release"
                   rows.group_by { |row| row.fetch("minecraft") }.values.flat_map { |group| [group.first, group.last].uniq }
                 else
                   rows
                 end
      selected.map do |row|
        {
          "target" => target.fetch("id"),
          "minecraft" => row.fetch("minecraft"),
          "voicechatArtifact" => row.fetch("voicechatArtifact")
        }
      end
    end
  end

  def server_matrix(catalog, mode)
    catalog.fetch("server").fetch("targets").sort_by { |target| Gem::Version.new(target.fetch("minecraft")) }.flat_map do |target|
      artifacts = target.fetch("voicechatArtifacts").uniq.sort_by { |artifact| artifact_order(artifact) }
      selected = case mode
                 when "smoke"
                   artifacts.empty? ? [] : [artifacts.last]
                 when "release"
                   artifacts.empty? ? [] : [artifacts.first, artifacts.last].uniq
                 else
                   artifacts
                 end
      %w[paper folia].select { |platform| target.key?("#{platform}Build") }.flat_map do |platform|
        selected.map do |artifact|
          {
            "platform" => platform,
            "minecraft" => target.fetch("minecraft"),
            "build" => target.fetch("#{platform}Build"),
            "voicechatArtifact" => artifact
          }
        end
      end
    end
  end

  def artifact_order(artifact)
    # Compare the SVC version before the Minecraft qualifier in either artifact naming scheme.
    [VoicechatVersionDiscovery.core(artifact), artifact.split(/(\d+)/).map { |part| part.match?(/\A\d+\z/) ? [1, part.to_i] : [0, part] }]
  end

  def validate_mode!(mode)
    raise ArgumentError, "unsupported compatibility mode: #{mode.inspect}" unless MODES.include?(mode)
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} COMPATIBILITY_JSON EVENT_NAME HEAD_REF INPUT_MODE CHANGED_FILES" unless ARGV.length == 5

  catalog_path, event_name, head_ref, input_mode, changed_path = ARGV
  catalog = JSON.parse(File.read(catalog_path))
  changed_files = File.readlines(changed_path, chomp: true)
  puts JSON.generate(CiCompatibilityPlan.build(catalog, event_name: event_name, head_ref: head_ref, input_mode: input_mode, changed_files: changed_files))
end
