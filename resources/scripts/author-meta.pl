# Get authros by language; load metadata
# IMPORTANT: $authname =~ s/\&/\%26/ - authors' names tarnsformed!

use LWP::UserAgent;
use XML::Simple;

my @langs = ("en", "ru", "es", "nl", "pt", "pl", "ca", "it", "fr", "de");
# my @langs = ("test");
my $AUTHID = 4;
my $AUTHNAME = 5; # or empty if $AUTHID contains IP

my $prefix = "http://$lang.dbpedia.org/resource/";
my $path = "fused-data";
# $input_trace = "$lang-big.out.trace";
	
# Media Wiki API tags:
my $query = "query";
my $users = "users";
my $user = "user";
my $name = "name";
my $editcount = "editcount";
my $registration = "registration";
my $blockinfo = "blockinfo";
my $groups = "groups";
my $gg = "g";

my $count = "count";

# (some) Wikipedia user groups
my %user_groups = ("sysop" => 0, "bot" => 1, "abusefilter" => 2, "bureaucrat" => 3, "autoconfirmed" => 4, "administrator" => 5, "reviewer" => 6);
my $header = "author\t$editcount\t$registration\t$blockinfo";
foreach $g (sort keys(%user_groups))
{
	$header = $header . "\t$g";
}
$header = $header . "\n";

my $fout = "authors-all";
open(O, ">$fout");
print O "lang\t$header";
open(S, ">authors-statistics");
open(L, ">authors-log");

my $ua = new LWP::UserAgent;
foreach $lang (@langs)
{
	open(OL, ">$lang-$fout");
	print OL "$header";
	
	my %authors = ();
	my %ips = ();
	my $authcnt = 0;
	my $auth_edits = 0;
	my $ipcnt = 0;
	my $ip_edits = 0;
	
	my $n = 0;
	my $input = "$path/$lang/$lang-big.out";
	open(I, $input);
	while ($line = <I>) 
	{
		# just logging...
		$n++;
		if ($n % 1000 == 0)
		{
			print "$lang, at $n-th line, authcnt = $authcnt\n";
		}
			
		$line =~ s/\n//;
		@tmp = split(/\t/, $line);	
		$authname = $tmp[$AUTHNAME];
		$authname =~ s/\&/\%26/;			
		if ($authname eq '')
		{
			if (not exists($ips{$tmp[$AUTHID]}))
			{
				$ips{$tmp[$AUTHID]} = 1;
				$ipcnt++;
			}
			else
			{
				$ips{$tmp[$AUTHID]}++;
			}
			$ip_edits++;
		}		
		else
		{		
			if (not exists($authors{$authname}))
			{
				$authors{$authname}{$count} = 1;
				$authcnt++;
				my $url = "http://" . $lang . ".wikipedia.org/w/api.php?action=query&list=users&ususers=" . $authname . "&usprop=blockinfo|groups|editcount|registration|rights&format=xml";
				# for test mode: my $url = "http://en.wikipedia.org/w/api.php?action=query&list=users&ususers=" . $authname . "&usprop=blockinfo|groups|editcount|registration|rights&format=xml";
				my $response = $ua->get($url);
				if ($response->is_success) 
				{
					$xml = new XML::Simple;
					$data = $xml->XMLin($response->decoded_content);
					
					if ($data->{$query}->{$users}->{$user}->{$name} ne $authname) 
					{
						print "error: $data->{$query}->{$users}->{$user}->{$name} not equal to $authname\n";
						print L "error: $data->{$query}->{$users}->{$user}->{$name} not equal to $authname\n";
					}
					# else # see, perhaps something is extracted (to check)
					{
						$authors{$authname}{$editcount} = $data->{$query}->{$users}->{$user}->{$editcount};
						$authors{$authname}{$registration} = $data->{$query}->{$users}->{$user}->{$registration};
						$authors{$authname}{$blockinfo} = $data->{$query}->{$users}->{$user}->{$blockinfo};
						foreach $g (@{$data->{$query}->{$users}->{$user}->{$groups}->{$gg}})
						{
							if (exists($user_groups{$g}))
							{
								$authors{$authname}{$g} = 1;
							}
						}	 
						
						my $str = "$authname\t$authors{$authname}{$editcount}\t$authors{$authname}{$registration}\t$authors{$authname}{$blockinfo}";
						foreach $g (sort keys(%user_groups))
						{
							$str = $str . "\t$authors{$authname}{$g}";
						}						
						print OL "$str\n";	
						print O "$lang\t$str\n";							
					}
				}
				else 
				{
					print "$authname\t$response->status_line\n";
					print L "$authname\t$response->status_line\n";
				}
			}
			else
			{
				$authors{$authname}{$count}++;
			}
			$auth_edits++;
		}		
	}	
	close(I);
	close(OL);

	print S "$lang, author count : $authcnt\n";
	print S "$lang, author edits : $auth_edits\n";
	print S "$lang, ip count : $ipcnt\n";
	print S "$lang, ip edits : $ip_edits\n";
}
close(O);
close(S);
close(L);
		