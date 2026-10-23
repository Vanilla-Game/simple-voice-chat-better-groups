#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "minitest/autorun"
require "open3"
require "rbconfig"
require "tmpdir"
require_relative "../../scripts/ci-compatibility-plan"

class CiCompatibilityPlanTest < Minitest::Test
  ROOT = File.expand_path("../..", __dir__)

  def setup
    @catalog = JSON.parse(File.read(File.join(ROOT, "compatibility.json")))
  end

  def test_catalog_has_small_pr_and_release_suites_and_full_covers_all_declared_pairs
    # New SVC patches discovered automatically must not require updating a
    # hard-coded full-matrix size just to get their compatibility PR green.
    full_counts = [
      @catalog.fetch("fabric").fetch("targets").sum { |target| target.fetch("compatibility").size },
      @catalog.fetch("server").fetch("targets").sum { |target| target.fetch("voicechatArtifacts").size * 2 }
    ]
    { "smoke" => [4, 4], "release" => [12, 8], "full" => full_counts }.each do |mode, counts|
      result = CiCompatibilityPlan.matrices(@catalog, mode: mode)
      assert_equal counts, [result.fetch("fabric").size, result.fetch("server").size], mode
      assert_equal %w[folia paper], result.fetch("server").map { |row| row.fetch("platform") }.uniq.sort
    end
  end

  def test_smoke_selects_latest_runtime_and_latest_voicechat_numerically
    result = CiCompatibilityPlan.matrices(unsorted_catalog, mode: "smoke")

    assert_equal [
      { "target" => "26.2", "minecraft" => "26.10", "voicechatArtifact" => "fabric-2.6.10+26.10" }
    ], result.fetch("fabric")
    assert_equal ["bukkit-2.6.10", "bukkit-2.6.10"], result.fetch("server").map { |row| row.fetch("voicechatArtifact") }
  end

  def test_release_tests_each_runtime_boundary_without_inventing_pairs
    result = CiCompatibilityPlan.matrices(unsorted_catalog, mode: "release")

    assert_equal [
      ["26.2", "fabric-2.6.9+26.2"],
      ["26.2", "fabric-2.6.10+26.2"],
      ["26.10", "fabric-2.6.8+26.10"],
      ["26.10", "fabric-2.6.10+26.10"]
    ], result.fetch("fabric").map { |row| row.values_at("minecraft", "voicechatArtifact") }
    assert_equal ["bukkit-2.6.8", "bukkit-2.6.10"] * 2, result.fetch("server").map { |row| row.fetch("voicechatArtifact") }
  end

  def test_single_version_boundaries_are_not_duplicated
    catalog = unsorted_catalog
    target = catalog.fetch("fabric").fetch("targets").first
    target["compatibility"] = [target.fetch("compatibility").first] * 2
    catalog.fetch("server").fetch("targets").first["voicechatArtifacts"] = ["bukkit-2.6.9"] * 2

    result = CiCompatibilityPlan.matrices(catalog, mode: "release")
    assert_equal 1, result.fetch("fabric").size
    assert_equal 2, result.fetch("server").size
  end

  def test_full_is_deterministic_and_other_tiers_are_subsets_without_mutating_catalog
    original = JSON.generate(@catalog)
    full = CiCompatibilityPlan.matrices(@catalog, mode: "full")
    shuffled = Marshal.load(Marshal.dump(@catalog))
    shuffled.fetch("fabric").fetch("targets").reverse!.each { |target| target.fetch("compatibility").reverse! }
    shuffled.fetch("server").fetch("targets").reverse!.each { |target| target.fetch("voicechatArtifacts").reverse! }
    assert_equal full, CiCompatibilityPlan.matrices(shuffled, mode: "full")

    %w[smoke release].each do |mode|
      result = CiCompatibilityPlan.matrices(@catalog, mode: mode)
      %w[fabric server].each do |side|
        assert_empty result.fetch(side) - full.fetch(side), "#{mode} #{side} contains a non-catalog combination"
        assert_equal result.fetch(side).uniq, result.fetch(side)
      end
    end
    assert_equal original, JSON.generate(@catalog)
  end

  def test_old_artifact_format_orders_voicechat_version_not_minecraft_prefix
    catalog = unsorted_catalog
    catalog.fetch("fabric").fetch("targets").first["compatibility"] = [
      { "minecraft" => "1.21.11", "voicechatArtifact" => "fabric-1.21.11-2.6.10" },
      { "minecraft" => "1.21.11", "voicechatArtifact" => "fabric-1.21.11-2.6.9" }
    ]
    row = CiCompatibilityPlan.matrices(catalog, mode: "smoke").fetch("fabric").first
    assert_equal "fabric-1.21.11-2.6.10", row.fetch("voicechatArtifact")
  end

  def test_client_only_and_server_only_changes_do_not_start_the_other_side
    client = plan(changed_files: ["client-fabric/src/main/java/Example.java"])
    assert_equal ["smoke", true, false], client.values_at("mode", "run-client", "run-server")
    assert_empty client.fetch("server")
    server = plan(changed_files: ["src/main/java/Example.java"])
    assert_equal ["smoke", false, true], server.values_at("mode", "run-client", "run-server")
    assert_empty server.fetch("fabric")
  end

  def test_changes_on_both_sides_run_both
    result = plan(changed_files: ["src/main/resources/config.yml", "client-fabric/src/main/resources/fabric.mod.json"])
    assert_equal ["smoke", true, true], result.values_at("mode", "run-client", "run-server")
  end

  def test_docs_only_and_unrelated_metadata_skip_runtime_checks
    result = plan(changed_files: %w[README.md docs/architecture.md CHANGELOG.md assets/demo.png .gitignore])
    assert_equal ["none", false, false, [], []], result.values_at("mode", "run-client", "run-server", "fabric", "server")
  end

  def test_shared_build_and_compatibility_changes_run_both_smoke_suites
    %w[
      compatibility.json build.gradle.kts settings.gradle.kts gradle.properties
      gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat
      .github/workflows/build.yml .github/workflows/release-please.yml
      .github/workflows/voicechat-compat.yml .github/workflows/voicechat-discovery.yml
      .github/workflows/server-compat.yml scripts/ci-compatibility-plan.rb
      scripts/voicechat-version-discovery.rb scripts/apply-voicechat-compatibility-results.rb
      test/scripts/ci_compatibility_plan_test.rb scripts/merge-compatibility-results.rb
      test/scripts/merge_compatibility_results_test.rb
    ].each do |path|
      assert_equal ["smoke", true, true], plan(changed_files: [path]).values_at("mode", "run-client", "run-server"), path
    end
  end

  def test_runtime_script_changes_select_the_affected_side
    assert_equal [true, false], plan(changed_files: ["scripts/run-client-compat.py"]).values_at("run-client", "run-server")
    assert_equal [true, false], plan(changed_files: ["test/scripts/test_run_client_compat.py"]).values_at("run-client", "run-server")
  end

  def test_release_branch_runs_both_boundaries_even_for_version_only_changes
    result = plan(head_ref: "release-please--branches--main--components--simple-voice-chat-better-groups", changed_files: ["CHANGELOG.md"])
    assert_equal ["release", true, true], result.values_at("mode", "run-client", "run-server")
    assert_equal [12, 8], result.values_at("fabric", "server").map(&:size)
  end

  def test_weekly_and_manual_override_paths_and_release_branch
    scheduled = plan(event_name: "schedule", changed_files: ["README.md"])
    assert_equal ["full", true, true], scheduled.values_at("mode", "run-client", "run-server")
    %w[none smoke release full].each do |mode|
      manual = plan(event_name: "workflow_dispatch", input_mode: mode, head_ref: "release-please--branches--main--components--plugin", changed_files: ["README.md"])
      assert_equal mode, manual.fetch("mode")
      assert_equal [mode != "none"] * 2, manual.values_at("run-client", "run-server")
    end
  end

  def test_main_push_keeps_runtime_checks_skipped
    assert_equal ["none", false, false], plan(event_name: "push", changed_files: ["src/main/java/Example.java"]).values_at("mode", "run-client", "run-server")
  end

  def test_invalid_modes_and_events_fail_instead_of_silently_running_a_different_tier
    assert_raises(ArgumentError) { CiCompatibilityPlan.matrices(@catalog, mode: "ful") }
    assert_raises(ArgumentError) { plan(event_name: "workflow_dispatch", input_mode: "") }
    assert_raises(ArgumentError) { plan(event_name: "pull_request_target") }
  end

  def test_cli_reports_the_same_plan_as_the_library
    Dir.mktmpdir do |directory|
      changed_path = File.join(directory, "changed-files.txt")
      File.write(changed_path, "src/main/java/Example.java\n")
      stdout, stderr, status = Open3.capture3(
        RbConfig.ruby, File.join(ROOT, "scripts/ci-compatibility-plan.rb"),
        File.join(ROOT, "compatibility.json"), "pull_request", "feature", "", changed_path
      )
      assert status.success?, stderr
      assert_equal plan(head_ref: "feature", changed_files: ["src/main/java/Example.java"]), JSON.parse(stdout)
    end
  end

  private

  def plan(**options)
    CiCompatibilityPlan.build(@catalog, **{ event_name: "pull_request" }.merge(options))
  end

  def unsorted_catalog
    {
      "fabric" => { "targets" => [{
        "id" => "26.2",
        "compatibility" => [
          { "minecraft" => "26.10", "voicechatArtifact" => "fabric-2.6.10+26.10" },
          { "minecraft" => "26.2", "voicechatArtifact" => "fabric-2.6.10+26.2" },
          { "minecraft" => "26.10", "voicechatArtifact" => "fabric-2.6.9+26.10" },
          { "minecraft" => "26.2", "voicechatArtifact" => "fabric-2.6.9+26.2" },
          { "minecraft" => "26.10", "voicechatArtifact" => "fabric-2.6.8+26.10" }
        ]
      }] },
      "server" => { "targets" => [{
        "minecraft" => "26.2", "paperBuild" => 10, "foliaBuild" => 2, "leafBuild" => 3,
        "voicechatArtifacts" => %w[bukkit-2.6.9 bukkit-2.6.10 bukkit-2.6.8]
      }] }
    }
  end
end
