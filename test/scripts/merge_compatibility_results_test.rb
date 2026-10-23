#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "minitest/autorun"
require "open3"
require "rbconfig"
require "tmpdir"
require_relative "../../scripts/merge-compatibility-results"

class MergeCompatibilityResultsTest < Minitest::Test
  FABRIC = "fabric__26.3__26.3__fabric-2.6.25+26.3"
  PAPER = "server__paper__26.2__bukkit-2.6.25"
  FOLIA = "server__folia__26.2__bukkit-2.6.25"
  ROOT = File.expand_path("../..", __dir__)

  def setup
    @directory = Dir.mktmpdir
    @input = File.join(@directory, "input")
    @output = File.join(@directory, "output")
    Dir.mkdir(@input)
  end

  def teardown
    FileUtils.remove_entry(@directory)
  end

  def test_latest_attempt_wins_while_unrerun_successes_are_preserved
    artifact("fabric-old", 1, FABRIC => "fail\n")
    artifact("paper-old", 1, PAPER => "ok\n")
    artifact("fabric-new", 10, FABRIC => "ok\n")
    artifact("fabric-middle", 2, FABRIC => "fail\n")

    assert_equal({ "merged" => 2 }, MergeCompatibilityResults.merge(@input, @output))
    assert_equal({ FABRIC => "ok\n", PAPER => "ok\n" }, output)
  end

  def test_latest_failure_is_not_hidden_by_an_older_success
    artifact("paper-old", 1, PAPER => "ok\n")
    artifact("paper-new", 2, PAPER => "fail\n")

    MergeCompatibilityResults.merge(@input, @output)
    assert_equal "fail\n", output.fetch(PAPER)
  end

  def test_conflicting_duplicates_at_highest_attempt_fail_before_writing_output
    artifact("one", 2, PAPER => "ok\n")
    artifact("two", 2, PAPER => "fail\n")

    error = assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    assert_match "conflicting results at attempt 2", error.message
    refute File.exist?(@output)
  end

  def test_identical_duplicates_and_superseded_conflicts_are_harmless
    artifact("one", 1, PAPER => "ok\n")
    artifact("two", 1, PAPER => "fail\n")
    artifact("three", 2, PAPER => "ok\n")
    artifact("four", 2, PAPER => "ok\r\n")

    MergeCompatibilityResults.merge(@input, @output)
    assert_equal({ PAPER => "ok\n" }, output)
  end

  def test_invalid_content_fails_even_when_the_artifact_has_been_superseded
    artifact("old", 1, FABRIC => "success\n")
    artifact("new", 2, FABRIC => "ok\n")

    error = assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    assert_match "invalid result value", error.message
    refute File.exist?(@output)
  end

  def test_unsafe_or_unrecognized_filenames_fail
    artifact("wrong", 1, "other.txt" => "ok\n")
    error = assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    assert_match "invalid result name", error.message
  end

  def test_symlinked_results_are_rejected
    directory = artifact("link", 1, {})
    target = File.join(@directory, "outside")
    File.write(target, "ok\n")
    File.symlink(target, File.join(directory, PAPER))

    error = assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    assert_match "expected regular result file", error.message
  end

  def test_attempt_suffix_is_required_and_must_be_positive
    ["compat-result-old", "compat-result-old-attempt-0", "compat-result-old-attempt--1"].each do |name|
      directory = File.join(@input, name)
      Dir.mkdir(directory)
      File.write(File.join(directory, PAPER), "ok\n")
      assert_raises(ArgumentError) { MergeCompatibilityResults.collect(@input) }
      FileUtils.remove_entry(directory)
    end
  end

  def test_empty_input_fails
    assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    refute File.exist?(@output)
  end

  def test_output_is_stable_regardless_of_artifact_creation_order
    artifact("z-first", 3, PAPER => "ok\n", FABRIC => "fail\n")
    artifact("a-second", 4, FABRIC => "ok\n", FOLIA => "ok")
    expected = MergeCompatibilityResults.collect(@input)
    FileUtils.remove_entry(@input)
    Dir.mkdir(@input)
    artifact("a-second", 4, FOLIA => "ok", FABRIC => "ok\n")
    artifact("z-first", 3, FABRIC => "fail\n", PAPER => "ok\n")
    assert_equal expected, MergeCompatibilityResults.collect(@input)
    assert_equal [FABRIC, FOLIA, PAPER].sort, expected.keys
  end

  def test_nonempty_output_fails_without_overwriting_existing_files
    artifact("paper", 1, PAPER => "ok\n")
    Dir.mkdir(@output)
    File.write(File.join(@output, PAPER), "fail\n")
    assert_raises(ArgumentError) { MergeCompatibilityResults.merge(@input, @output) }
    assert_equal "fail\n", File.read(File.join(@output, PAPER))
  end

  def test_cli_writes_results_and_reports_count
    artifact("paper", 1, PAPER => "ok\n")
    stdout, stderr, status = Open3.capture3(
      RbConfig.ruby, File.join(ROOT, "scripts/merge-compatibility-results.rb"), @input, @output
    )
    assert status.success?, stderr
    assert_equal({ "merged" => 1 }, JSON.parse(stdout))
    assert_equal({ PAPER => "ok\n" }, output)
  end

  private

  def artifact(name, attempt, files)
    directory = File.join(@input, "compat-result-#{name}-attempt-#{attempt}")
    Dir.mkdir(directory)
    files.each { |file, value| File.write(File.join(directory, file), value) }
    directory
  end

  def output
    Dir.children(@output).sort.to_h { |name| [name, File.read(File.join(@output, name))] }
  end
end
