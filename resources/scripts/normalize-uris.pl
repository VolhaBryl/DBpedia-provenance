# ...normalize uris to en, take unicode issues into account, produce new .out files with sbj and value mappings in the beggining of each string...
use utf8;
use Encode::Escape;
use URI::Escape;
binmode STDOUT, ":utf8";  

# languages:
my @langs = ("en", "ru", "es", "nl", "pt", "pl", "ca", "it", "fr", "de");
my $en_str = "en";
# files:
my $path = "fused-data";
my $cpath = "concepts";
my $foutmapp = "out-mapping";
my $foutinters = "out-intesection";
my $flinks = "interlinks/interlanguage_links_en_filtered.nt";
open(L, ">normalization-log"); # log file
# indexes:
my $ISUBJ = 0; # in .out files
my $IVAL = 2; # in .out files
my $IEN = 0; # in $flinks
my $ILANG = 2; # in $flinks
my $STR = "str"; # for %unique
my $CNT = "cnt"; # for %unique
# prefixes
my $norm_prefix = "http://dbpedia.org/resource/";
my $prefix_en = "<http://dbpedia.org/resource/";
my $prefix1 = "http:\/\/";
my $prefix2 = "\.dbpedia\.org\/resource\/";
# my $postfix = "\> \.\n";

# language-dependent paths and prefixes:
sub getOutFile
{
	my $lang = $_[0];
	return "$path/$lang/$lang-big.out.nn";
}
sub getPrefix
{
	my $lang = $_[0];
	return "http://$lang.dbpedia.org/resource/";
}

# Hash maps to store the mapping results
%big = ();
%unique = ();
%nonentrans = ();
%entrans = ();
%nonentrans_rev = ();
%entrans_rev = ();

# step 1, get lists of used concepts, cope with encodings:
foreach $lang (@langs)
{
	print "step 1, $lang\n";
	
	$prefix = &getPrefix($lang);
	$fout = &getOutFile($lang);

	#open(O, ">$cpath/$lang-concepts");
	#open(V, ">$cpath/$lang-values");
	%concepts = ();
	%invalues = ();

	$counts = 0;
	$countv = 0;
	open(I, $fout);
	while ($line = <I>) 
	{
		@tmp = split(/\t/, $line);	
		$sbj = $tmp[$ISUBJ];
		$val = $tmp[$IVAL];
		
		# subject:
		$sbj =~ s/$prefix//;
		if (not exists $concepts{$sbj})
		{
			$concepts{$sbj} = 0;		
			# Function call
			$new_key = &ConvertStr($sbj, $lang);			
			$big{$lang}{$new_key} = "";
			$counts++;
		}
		
		# value:
		if ($val =~ /$prefix/)
		{
			$val =~ s/$prefix//;
			# if (not exists $concepts{$val})
			if (not exists $invalues{$val})
			{
				$invalues{$val} = 0;	
				$new_key = &ConvertStr($val, $lang);			
				$big{$lang}{$new_key} = "";				
				$countv++; 
			}
		}
	}
	close(I);

	#print L "$lang, subjects: $counts\n";
	#print L "$lang, values: $countv\n";
	#$counts = 0;
	#$countv = 0;
	#foreach $c (keys(%concepts))
	#{
	#	print O "$c\n";	
	#	$counts++;
	#}
	#foreach $c (keys(%invalues))
	#{
	#	if (not exists $concepts{$c})
	#	{
	#		print V "$c\n";	
	#		$countv++;
	#	}
	#}
	#print L "$lang, subjects, post: $counts\n";
	#print L "$lang, values, post: $countv\n";
	#close(O);
	#close(V);
}

# step 2, see intelinks, create mappings, construct concept intersection:
open(I, $flinks);
while ($line = <I>) 
{
	if ($line =~ /$prefix1/)
	{
		@tmp = split(/\> \</, $line);	
		$en = $tmp[$IEN];
		$ln = $tmp[$ILANG];
		# if ($ln =~ /$prefix1(..)$prefix2(.+)$postfix/)
		if ($ln =~ /$prefix1(..)$prefix2(.+)(...)/)
		{
			$lang = $1;
			$concept = $2;
		
			if (exists($big{$lang}{$concept}))
			{
				$en =~ s/\n//g;
				$en =~ s/$prefix_en//;
				$big{$lang}{$concept} = $en;
				
				if (exists($unique{$en}))
				{
					$unique{$en}{$STR} = $unique{$en}{$STR} . "+" . $lang;
					$unique{$en}{$CNT}++;
				}
				else
				{
					$unique{$en}{$STR} = $lang;
					$unique{$en}{$CNT} = 1;
				}
			}
		}
	}
}
close(I);

# interlinks file does not contain sameAs statements for English, so those to be added additionally:
foreach $en (keys($big{$en_str}))
{
	if (exists($unique{$en}))
	{
		$unique{$en}{$STR} = $unique{$en}{$STR} . "+" . $en_str;
		$unique{$en}{$CNT}++;
	}
	else
	{
		$unique{$en}{$STR} = $en_str;
		$unique{$en}{$CNT} = 1;
	}
	$big{$en_str}{$en} = $en;
}

open(O, ">$foutinters");
my %stat = ();
foreach $c (keys(%unique))
{
	print O "$c\t$unique{$c}{$CNT}\t$unique{$c}{$STR}\n";
	# calculate statistics:
	foreach $s ($unique{$c}{$CNT},$unique{$c}{$STR})
	{
		if (exists($stat{$s}))
		{
			$stat{$s}++;
		}
		else
		{
			$stat{$s} = 1;
		}
	}
}
# print statistics:
print O "\n\n";
foreach $s (keys(%stat))
{
	print O "$s\t$stat{$s}\n";
}
close(O);

# step 2a: print mappings
open(O, ">$foutmapp");
print O "lang\tinitial-key\ttranformed-key\ten-key\tmulti-lang count\n";
foreach $lang (@langs)
{
	foreach $k (keys($big{$lang}))
	{
		$initial = &GetInitialKey($k, $lang);
		$s = $big{$lang}{$k};
		# if ($unique{$s}{$CNT} > 1)
		{
			print O "$lang\t$initial\t$k\t$s\t$unique{$s}{$CNT}";
		}
	}
}
close(O);

# step 3 : reorganize out files: add 2 fields at the beginning of the string for subject and value mappings to standard (en) DBpedia URIs
foreach $lang (@langs)
{
	print "step 3, $lang\n";
	
	$prefix = &getPrefix($lang);
	$fout = &getOutFile($lang);

	open(I, $fout);
	open(O, ">$fout.norm");
	while ($line = <I>) 
	{
		$line =~ s/\n//; 
		
		@tmp = split(/\t/, $line);	
		$sbj = $tmp[$ISUBJ];
		$val = $tmp[$IVAL];
		
		$sbj =~ s/$prefix//;
		$k_sbj = &GetEncodedKey($sbj, $lang);
		my $pre = "\t";
		if (exists($big{$lang}{$k_sbj}) and $big{$lang}{$k_sbj} ne "")
		{
			$pre = "$norm_prefix$big{$lang}{$k_sbj}\t";
		}
		else
		{
			print L "not found subject mapping : $lang, $sbj, $k_sbj\n"
		}
		# do value matching only if value is a lang-DBpedia concept:
		if ($val =~ /$prefix/)
		{
			$val =~ s/$prefix//;
			$k_val = &GetEncodedKey($val, $lang);
			if (exists($big{$lang}{$k_val}) and $big{$lang}{$k_val} ne "")
			{
				$pre = $pre . "$norm_prefix$big{$lang}{$k_val}";
			}		
			else
			{
				print L "not found value mapping : $lang, $val, $k_val\n"
			}
		}
		$pre = $pre . "\t";
		#
		print O "$pre$line";		
	}
	close(O);
	close(I);
}
close(L);


############################### FUNCTIONS ############################################

# Converts 
# 		an English DBpedia concept name (e.g. in Cyrillic) into a name with uri-escaped symbols (%C3)
# 		a non-English name into unicode-java-style symbols (\u4545)
#		saves mappings into %entrans and %nonentrans
# Input: unicode DBpedia concept name, lang 
# Output: new concept name, to be looked up in interlinks DBpedia dumps
sub ConvertStr
{
   my $str = $_[0];
   my $lang = $_[1];
   
   my $output = "";
   if ($lang eq $en_str)
   {
		# conversion from utf to uris (with %):
		$output = decode 'utf8', $str;
		$output = uri_escape_utf8($output);
		#
		if ($str ne $output)
		{
			$entrans{$output} = $str;
			$entrans_rev{$str} = $output;
		}
   }
   else
   {
		# conversion from utf to \uxxxx codes:
		$output = decode 'utf8', $str;
		$output = encode 'unicode-escape', $output;
		$output =~ s/\\x\{(....)\}/\\u$1/g;	
		#
		if ($str ne $output)
		{
			$nonentrans{$output} = $str;
			$nonentrans_rev{$str} = $output;
		}
   } 
   return $output;
}

# Reconstructs a transormed key (from unicode-java-style symbols (\u4545) and uri-escapes (%C3) into initial value (as it was in .out))
# Input: key string, lang 
# Output: initial key string
sub GetInitialKey
{
	my $k = $_[0];
	my $lang = $_[1];
	my $initial = $k;
    if ($lang eq $en_str)
	{
		if (exists($entrans{$k}))
		{
			$initial = $entrans{$k};
		}
	}
	else
	{
		if (exists($nonentrans{$k}))
		{
			$initial = $nonentrans{$k};
		}
	}		
   return $initial;
}

# Gives previoulsy stored encoded key (with unicode-java-style symbols and uri-escapes)
# Input: key string, lang 
# Output: encoded key string
sub GetEncodedKey
{
	my $k = $_[0];
	my $lang = $_[1];
	my $encoded = $k;
    if ($lang eq $en_str)
	{
		if (exists($entrans_rev{$k}))
		{
			$encoded = $entrans_rev{$k};
		}
	}
	else
	{
		if (exists($nonentrans_rev{$k}))
		{
			$encoded = $nonentrans_rev{$k};
		}
	}		
   return $encoded;
}