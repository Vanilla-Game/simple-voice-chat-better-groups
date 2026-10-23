#!/usr/bin/env ruby
# frozen_string_literal: true

require "fileutils"
require "json"

module MergeCompatibilityResults
  ARTIFACT_NAME = /\Acompat-result-.+-attempt-([1-9]\d*)\z/
  RESULT_NAME = /\A(?:fabric|server)__[A-Za-z0-9][A-Za-z0-9_.+-]*__[A-Za-z0-9][A-Za-z0-9_.+-]*__[A-Za-z0-9][A-Za-z0-9_.+-]*\z/

  module_function

  def merge(input_directory, output_directory)
    results = collect(input_directory)
    if File.exist?(output_directory) && (!File.directory?(output_directory) || !Dir.empty?(output_directory))
      raise ArgumentError, "output directory must be empty: #{output_directory}"
    end

    FileUtils.mkdir_p(output_directory)
    results.sort.each do |name, result|
      File.write(File.join(output_directory, name), "#{result}\n")
    end
    { "merged" => results.size }
  end

  def collect(input_directory)
    selected = {}
    Dir.children(input_directory).sort.each do |artifact|
      next unless artifact.start_with?("compat-result-")

      match = ARTIFACT_NAME.match(artifact)
      raise ArgumentError, "artifact has no positive attempt suffix: #{artifact}" unless match

      directory = File.join(input_directory, artifact)
      raise ArgumentError, "expected artifact directory: #{artifact}" unless File.directory?(directory) && !File.symlink?(directory)

      attempt = match[1].to_i
      Dir.children(directory).sort.each do |name|
        raise ArgumentError, "invalid result name: #{name.inspect}" unless RESULT_NAME.match?(name)

        path = File.join(directory, name)
        raise ArgumentError, "expected regular result file: #{path}" unless File.file?(path) && !File.symlink?(path)

        result = File.binread(path).sub(/\r?\n\z/, "")
        raise ArgumentError, "invalid result value in #{path}" unless %w[ok fail].include?(result)

        previous = selected[name]
        if previous.nil? || attempt > previous.fetch(:attempt)
          selected[name] = { attempt: attempt, values: [result] }
        elsif attempt == previous.fetch(:attempt)
          previous.fetch(:values) << result
        end
      end
    end
    raise ArgumentError, "no compatibility results in #{input_directory}" if selected.empty?

    selected.sort.to_h do |name, entry|
      values = entry.fetch(:values).uniq
      raise ArgumentError, "conflicting results at attempt #{entry.fetch(:attempt)} for #{name}" unless values.size == 1

      [name, values.first]
    end
  end
end

if $PROGRAM_NAME == __FILE__
  abort "usage: #{$PROGRAM_NAME} INPUT_DIRECTORY OUTPUT_DIRECTORY" unless ARGV.length == 2

  puts JSON.generate(MergeCompatibilityResults.merge(ARGV.fetch(0), ARGV.fetch(1)))
end
