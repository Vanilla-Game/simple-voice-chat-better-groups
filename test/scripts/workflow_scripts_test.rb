#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "json"
require "minitest/autorun"
require "open3"
require "rbconfig"
require "tmpdir"

ROOT = File.expand_path("../..", __dir__)
FIXTURES = File.join(ROOT, "test", "fixtures", "workflows")

require File.join(ROOT, "scripts", "apply-voicechat-compatibility-results")
require File.join(ROOT, "scripts", "modrinth-featured-versions")
require File.join(ROOT, "scripts", "voicechat-version-discovery")

class WorkflowScriptsTest < Minitest::Test
  EXPECTED_VERSIONS = [
    "paper-0.9.2+26.1.2",
    "fabric-0.9.2+1.21.11",
    "fabric-0.9.2+26.1",
    "fabric-0.9.2+26.2"
  ].freeze

  def test_discovery_keeps_only_contiguous_patch_releases_in_the_same_minor
    result = VoicechatVersionDiscovery.build(
      fixture("discovery-catalog.json"),
      fixture("modrinth-versions.json")
    )

    assert_equal fixture("discovery-expected.json"), result
  end

  def test_discovery_rejects_artifacts_without_a_three_part_version
    error = assert_raises(ArgumentError) { VoicechatVersionDiscovery.core("fabric-latest") }

    assert_match "cannot extract a three-part version", error.message
  end

  def test_compatibility_update_stops_each_target_at_its_first_failure
    catalog = fixture("discovery-catalog.json")
    discovery = fixture("discovery-expected.json")

    result = VoicechatCompatibilityUpdate.apply(
      catalog,
      discovery.fetch("fabric-candidates"),
      discovery.fetch("server-candidates"),
      File.join(FIXTURES, "results")
    )

    assert_equal fixture("widen-expected-catalog.json"), catalog
    assert_equal fixture("widen-expected-result.json"), result
  end

  def test_compatibility_update_cli_writes_the_catalog_and_result
    Dir.mktmpdir do |directory|
      catalog_path = File.join(directory, "compatibility.json")
      fabric_path = File.join(directory, "fabric.json")
      server_path = File.join(directory, "server.json")
      results_path = File.join(directory, "results")
      discovery = fixture("discovery-expected.json")

      FileUtils.cp(File.join(FIXTURES, "discovery-catalog.json"), catalog_path)
      File.write(fabric_path, JSON.generate(discovery.fetch("fabric-candidates")))
      File.write(server_path, JSON.generate(discovery.fetch("server-candidates")))
      FileUtils.cp_r(File.join(FIXTURES, "results"), results_path)

      stdout, stderr, status = Open3.capture3(
        RbConfig.ruby,
        File.join(ROOT, "scripts", "apply-voicechat-compatibility-results.rb"),
        catalog_path,
        fabric_path,
        server_path,
        results_path
      )

      assert status.success?, stderr
      assert_equal fixture("widen-expected-catalog.json"), JSON.parse(File.read(catalog_path))
      assert_equal fixture("widen-expected-result.json"), JSON.parse(stdout)
    end
  end

  def test_modrinth_cleanup_selects_only_older_compatible_featured_versions
    ids = ModrinthFeaturedVersions.unfeature_ids(
      fixture("featured-versions.json"),
      EXPECTED_VERSIONS
    )

    assert_equal fixture("featured-expected-ids.json"), ids
  end

  def test_modrinth_cleanup_fails_if_a_published_version_is_missing
    error = assert_raises(ArgumentError) do
      ModrinthFeaturedVersions.unfeature_ids(
        fixture("featured-versions.json"),
        EXPECTED_VERSIONS + ["fabric-0.9.2+missing"]
      )
    end

    assert_match "fabric-0.9.2+missing", error.message
  end

  private

  def fixture(name)
    JSON.parse(File.read(File.join(FIXTURES, name)))
  end
end
