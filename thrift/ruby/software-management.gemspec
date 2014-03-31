Gem::Specification.new do |s|
  s.name = "software-management"
  s.version = File.exist?('VERSION') ? File.read('VERSION') : ""
  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["VMware Serengeti Team"]
  s.date = "2014-4-1"
  s.description = "Expose Thrift Interface for Ironfan. Serengete Web Service is the client."
  s.email = "serengeti-dev@googlegroups.com"
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
  s.require_paths = ["lib"]
  s.add_dependency(%q<thrift>,["~> 0.9.0"])
  s.add_dependency(%q<thin>,["~> 1.3.0"])
end
