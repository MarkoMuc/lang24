.global _write
_write:
	sd fp, 32(sp)	# Save old FP
	mv fp, sp	# New FP points to the start of this frame
	sd ra, 32(fp)	# Save ra, this might not be needed
	sd a0, 40(fp)	# Save used regs
	sd a1, 48(fp)
	sd a2, 56(fp)
	sd a7, 64(fp)

	ld a0, 8(fp)	# int fd
	ld a1, 16(fp)	# const void buffer
	ld a2, 24(fp)	# size_t counter
	li a7, 64	# 64 is the linux system call number for write
	ecall		# System call
	
	ld a0, 0(fp)	# Return value

	sd ra, 32(fp)	# Save ra, this might not be needed
	sd a0, 40(fp)	# Restore used regs
	sd a1, 48(fp)
	sd a2, 56(fp)
	sd a7, 64(fp)
	mv sp, fp	# Restore old SP
	ld fp, 32(sp)	# Restore old FP
	ret		# Returns from the subroutine
	
