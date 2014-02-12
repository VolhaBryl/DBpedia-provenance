# ...add author metatada to out.n (out with editcounts from trace)...

my $path = "fused-data";
# my $fauthors = "author\_metadata/authors-all";
my $fauthors = "author\_metadata/authors-all-minus2col";

my %authors = ();

# format: lang	author	editcount	registration	blockinfo	abusefilter	administrator	autoconfirmed	bot	bureaucrat	reviewer	sysop
open(A, $fauthors );
my $line = <A>;
while ($line = <A>) 
{
	# $line =~ s/\n//;
	@tmp = split(/\t/, $line);
	$lang = $tmp[0]; 
	$authname = $tmp[1]; 
	$line =~ s/$lang\t\Q$authname\E\t//; # cut 1st 2 fields
	$authname =~ s/\%26/\&/; # reverse formatting : $authname =~ s/\&/\%26/;
	$authors{$lang}{$authname} = $line;
}
close(A);

my @langs = ("en", "ru", "es", "nl", "pt", "pl", "ca", "it", "fr", "de");
my $empty = "\t\t\t\t\t\t\t\t\t\t\n";
my $AUTHID = 5;
foreach $lang (@langs)
{
	print "processing $lang...\n";
	
	$prefix = "http://$lang.dbpedia.org/resource/";

	$fout = "$path/$lang/$lang-big.out.n";
	$fnew = "$path/$lang/$lang-big.out.nn";

	open(out, $fout );
	open(nout, ">$fnew");
	while ($line = <out>) 
	{
		if ($line =~ /$prefix/)
		{
			$line =~ s/\n//;
			@tmp = split(/\t/, $line);	
			$auth = $tmp[$AUTHID];
			if (exists($authors{$lang}{$auth}))
			{
				print nout "$line\t$authors{$lang}{$auth}";
			}
			else
			{
				print nout "$line\t$empty";
			}
		}
	}
	close(out);
	close(nout);
}