Gem::Specification.new do |s|
  s.name = "software-management"
  s.version = File.exist?('VERSION') ? File.read('VERSION') : ""
  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["VMware Serengeti team (Jarred Li)"]
  s.date = "2013-02-07"
  s.description = "Expose Thrift service for software management. Serengete web service is the client"
  s.email = "hadoop-bj@vmware.com"
  s.extra_rdoc_files = [
    "LICENSE",
    "README.rdoc"
  ]
  s.files = [
    "LICENSE",
    "README.rdoc",
    "software-management.gemspec",
    "lib/server/software_management_server.rb",
    "lib/server/software_management_handler.rb",
    "lib/server/progress_monitor.rb",
    "lib/software_management.rb",
    "lib/software_management_types.rb",
    "lib/software_management_constants.rb"
  ]
  s.homepage = "http://serengeti.cloudfoundry.org"
  s.licenses = ["apachev2"]
  s.summary = "Expose Thrift service for software management"
  s.require_path = ["lib"]
  s.rubygems_version = "1.8.15"
  s.add_dependency(%q<thrift>,["~> 0.9.0"])
end
