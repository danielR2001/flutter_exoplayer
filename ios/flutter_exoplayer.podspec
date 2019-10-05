#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_exoplayer'
  s.version          = '0.5.2'
  s.summary          = 'A flutter plugin to play audio files using the Java ExoPlayer library.'
  s.description      = <<-DESC
A flutter plugin to play audio files using the Java ExoPlayer library.'
                       DESC
  s.homepage         = 'https://github.com/danielR2001/flutter_exoplayer'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Daniel Rachlin' => 'daniel.rachlin@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'

  s.ios.deployment_target = '8.0'
end

